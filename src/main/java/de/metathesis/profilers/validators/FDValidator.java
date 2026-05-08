package de.metathesis.profilers.validators;

import de.metathesis.profilers.structures.FDTreeNode;
import de.metathesis.profilers.structures.NegativeCover;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FDValidator {
    private static final Logger log = LogManager.getLogger(FDValidator.class);

    @Getter
    private final int numAttributes;
    private final int[][] compressed;
    private final PositionListIndex[] plis;

    private final double validationThreshold = 0.01;

    @Getter
    private boolean isInitialValidation = true;

    private final FDTreeNode root;

    public FDValidator(Relation relation) {
        this.numAttributes = relation.getNumOfAttributes();
        this.compressed = relation.getCompressedRecords();
        this.plis = relation.getUnaryPLIs();

        this.root = new FDTreeNode(numAttributes);
        //Initialize Add Most General Dependencies
        this.root.getRhsCandidateFds().set(0, numAttributes);
        this.root.getRhsAttributes().set(0, numAttributes);
    }

    private class ValidationTask implements Callable<ValidationResult> {
        private final BitSet lhs;
        private final BitSet rhs;

        public ValidationTask(BitSet lhs, BitSet rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public ValidationResult call() {
            // Empty LHS — check if RHS attributes are constants.
            if (lhs.isEmpty()) {
                BitSet validRhs = (BitSet) rhs.clone();
                for (int rhs = this.rhs.nextSetBit(0); rhs >= 0; rhs = this.rhs.nextSetBit(rhs + 1)) {
                    if (!FDValidator.this.plis[rhs].isConstant()) {
                        validRhs.clear(rhs);
                    }
                }
                return new ValidationResult(lhs, rhs, validRhs, Collections.emptySet());
            }

            Set<IntIntImmutablePair> suggestions = new HashSet<>();
            BitSet validRhs = (BitSet) rhs.clone();

            int firstLhsAttribute = lhs.nextSetBit(0);
            BitSet remainingLhs = (BitSet) lhs.clone();
            remainingLhs.clear(firstLhsAttribute);

            for (IntArrayList cluster : FDValidator.this.plis[firstLhsAttribute].getClusters()) {
                Object2ObjectMap<IntArrayList, Int2IntMap> seen = new Object2ObjectOpenHashMap<>();
                Object2IntMap<IntArrayList> representative = new Object2IntOpenHashMap<>();

                for (int record : cluster) {
                    IntArrayList key = Utility.buildKey(remainingLhs, FDValidator.this.compressed[record]);
                    if (key == null) {
                        continue;
                    }

                    if (seen.containsKey(key)) {
                        Int2IntMap existingRhs = seen.get(key);
                        for (int r = validRhs.nextSetBit(0); r >= 0; r = validRhs.nextSetBit(r + 1)) {
                            if (FDValidator.this.compressed[record][r] == -1 || FDValidator.this.compressed[record][r] != existingRhs.get(r)) {
                                suggestions.add(new IntIntImmutablePair(record, representative.getInt(key)));
                                validRhs.clear(r);
                                if (validRhs.isEmpty()) {
                                    return new ValidationResult(lhs, rhs, validRhs, suggestions);
                                }
                            }
                        }
                    } else {
                        Int2IntMap rhsVals = new Int2IntOpenHashMap();
                        for (int rhsAttr = validRhs.nextSetBit(0); rhsAttr >= 0; rhsAttr = validRhs.nextSetBit(rhsAttr + 1)) {
                            rhsVals.put(rhsAttr, FDValidator.this.compressed[record][rhsAttr]);
                        }
                        seen.put(key, rhsVals);
                        representative.put(key, record);
                    }
                }
            }

            return new ValidationResult(lhs, rhs, validRhs, suggestions);
        }
    }

    private record ValidationResult(
            BitSet lhs,
            BitSet rhs,
            BitSet validRhs,
            Set<IntIntImmutablePair> suggestions
    ) {}

    public Set<IntIntImmutablePair> validateFreeFree(ExecutorService executor,
                                                     NegativeCover newNegativeCover,
                                                     int level,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> results) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Map<BitSet, BitSet> candidates = this.root.getLhsPaths(level);

        List<Future<ValidationResult>> futures = new ArrayList<>(candidates.size());

        for (Map.Entry<BitSet, BitSet> candidate : candidates.entrySet()) {
            futures.add(executor.submit(new ValidationTask((BitSet) candidate.getKey().clone(), (BitSet) candidate.getValue().clone())));
        }

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount   = 0;
        int invalidFDCount = 0;

        for (Future<ValidationResult> future : futures) {
            ValidationResult result = future.get();

            BitSet lhs = result.lhs();
            BitSet rhs = result.rhs();
            BitSet validRhs = result.validRhs();
            totalCandidateCount += rhs.cardinality();

            // Invalid RHS bits — specialize posCover
            BitSet invalidRhs = (BitSet) rhs.clone();
            invalidRhs.andNot(validRhs);
            invalidFDCount += invalidRhs.cardinality();

            // Valid RHS bits — confirm in posCover and store results
            if (!validRhs.isEmpty()) {
                results.add(new ObjectObjectImmutablePair<>(lhs, validRhs));
                this.root.markAsValidate(lhs, validRhs);
            }

            for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                this.root.specializePositiveCover(lhs, attr);
            }

            if (!invalidRhs.isEmpty()) {
                suggestions.addAll(result.suggestions());
            }
        }

        if (invalidFDCount > totalCandidateCount * validationThreshold) {
            log.info("Back to Sampling | TC: {}, IC: {}, VE: {}", totalCandidateCount, invalidFDCount, totalCandidateCount * validationThreshold);
            return suggestions;
        }

        return null;
    }

    public Set<IntIntImmutablePair> validateFreeLock(NegativeCover newNegativeCover,
                                                     Set<BitSet> rhsCandidateList,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList){
        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validFDCount = 0;
        int invalidFDCount = 0;

        Iterator<BitSet> rhsCandidateIterator = rhsCandidateList.iterator();

        while (rhsCandidateIterator.hasNext()) {
            BitSet rhsCandidate = rhsCandidateIterator.next();
            Map<Integer, List<BitSet>> minimalLhsPerBit = new HashMap<>();
            boolean rhsCandidateValid = true;

            for (int rhsAttr = rhsCandidate.nextSetBit(0); rhsAttr >= 0; rhsAttr = rhsCandidate.nextSetBit(rhsAttr + 1)) {
                LinkedHashSet<ObjectObjectImmutablePair<BitSet, Boolean>> lhsCandidates = this.root.getLhsPathsForRhs(rhsCandidate, rhsAttr);
                log.info("Rhs: {} Found lhs candidates: {}", rhsAttr, lhsCandidates.size());

                BitSet rhsBit = new BitSet();
                rhsBit.set(rhsAttr);

                List<BitSet> confirmedLhsForBit = new ArrayList<>();

                while (!lhsCandidates.isEmpty()) {
                    ObjectObjectImmutablePair<BitSet, Boolean> candidate = lhsCandidates.removeFirst();
                    BitSet lhsCandidate = candidate.left();

                    if (candidate.right()) {
                        confirmedLhsForBit.add(lhsCandidate); // Already validated in tree
                        continue;
                    }

                    ValidationTask task = new ValidationTask(lhsCandidate, rhsBit);
                    ValidationResult result = task.call();

                    if (result.validRhs().equals(rhsBit)) {
                        this.root.markAsValidate(lhsCandidate, rhsBit);
                        confirmedLhsForBit.add(lhsCandidate);
                        validFDCount++;
                    } else {
                        BitSet specialized = this.root.specializePositiveCover(lhsCandidate, rhsAttr);
                        specialized.andNot(rhsCandidate);
                        log.info("Rhs: {} Invalid Lhs: {} Specialized: {}", rhsAttr, lhsCandidate, specialized);

                        for (int attr = specialized.nextSetBit(0); attr >= 0; attr = specialized.nextSetBit(attr + 1)) {
                            BitSet newLhs = (BitSet) lhsCandidate.clone();
                            newLhs.set(attr);
                            lhsCandidates.add(new ObjectObjectImmutablePair<>(newLhs, false));
                        }

                        suggestions.addAll(result.suggestions());
                        invalidFDCount++;
                    }
                }

                if (confirmedLhsForBit.isEmpty()) {
                    // This rhs bit has no valid minimal lhs therefore entire rhsCandidate has no solution
                    rhsCandidateValid = false;
                    break;
                }

                minimalLhsPerBit.put(rhsAttr, confirmedLhsForBit);
            }

            log.info("All Rhs validated lhs find is done! {}", rhsCandidateValid);

            if (rhsCandidateValid) {
                //Create the final result using iterative merging
                List<BitSet> result = null;

                for (int rhsAttr = rhsCandidate.nextSetBit(0); rhsAttr >= 0; rhsAttr = rhsCandidate.nextSetBit(rhsAttr + 1)) {
                    List<BitSet> bitPaths = minimalLhsPerBit.get(rhsAttr);
                    if (result == null) {
                        result = new ArrayList<>(bitPaths);
                    } else {
                        result = minimalCrossProduct(result, bitPaths);
                    }
                }

                if (result != null) {
                    for (BitSet mergedLhs : result) {
                        confirmList.add(new ObjectObjectImmutablePair<>(mergedLhs, rhsCandidate));
                    }
                }
            }

            rhsCandidateIterator.remove();

            if (validFDCount > 0 && invalidFDCount > validFDCount * validationThreshold) {
                return suggestions;
            }
        }

        return null;
    }

    public Set<IntIntImmutablePair> validateLockFree(NegativeCover newNegativeCover,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> pendingList,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) {

        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validFDCount   = 0;
        int invalidFDCount = 0;

        Iterator<ObjectObjectImmutablePair<BitSet, BitSet>> iterator = pendingList.iterator();

        while (iterator.hasNext()) {
            ObjectObjectImmutablePair<BitSet, BitSet> candidate = iterator.next();
            BitSet lhs = candidate.left();
            BitSet rhs = candidate.right();

            ObjectObjectImmutablePair<BitSet, BitSet> status = this.root.getRhsForLhsPaths(lhs, rhs);
            BitSet confirmedRhs = status.left();
            BitSet remainingRhs = status.right();

            if (confirmedRhs.isEmpty() && remainingRhs.isEmpty()) {
                //Nothing is confirmed and Nothing is remaining to confirm
                iterator.remove();
                continue;
            }

            if (remainingRhs.isEmpty()) {
                confirmList.add(new ObjectObjectImmutablePair<>(lhs, confirmedRhs));
                iterator.remove();
                continue;
            }

            // Validate remaining bits directly
            ValidationTask task = new ValidationTask(lhs, remainingRhs);
            ValidationResult result = task.call();

            BitSet validRhs   = result.validRhs();
            BitSet invalidRhs = (BitSet) remainingRhs.clone();
            invalidRhs.andNot(validRhs);

            validFDCount   += validRhs.cardinality();
            invalidFDCount += invalidRhs.cardinality();

            if (!validRhs.isEmpty()) {
                this.root.markAsValidate(lhs, validRhs);
                confirmedRhs.or(validRhs);
            }

            confirmList.add(new ObjectObjectImmutablePair<>(lhs, confirmedRhs));
            iterator.remove();

            if (!invalidRhs.isEmpty()) {
                for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                    this.root.specializePositiveCover(lhs, attr);
                }

                suggestions.addAll(result.suggestions());

                if (validFDCount > 0 && invalidFDCount > validFDCount * validationThreshold) {
                    return suggestions;
                }
            }
        }

        return (pendingList.isEmpty() && suggestions.isEmpty()) ? null : suggestions;
    }

    public Set<IntIntImmutablePair> validateLockLock(NegativeCover newNegativeCover,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> pendingList,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) {

        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validFDCount   = 0;
        int invalidFDCount = 0;

        Iterator<ObjectObjectImmutablePair<BitSet, BitSet>> iterator = pendingList.iterator();

        while (iterator.hasNext()) {
            ObjectObjectImmutablePair<BitSet, BitSet> candidate = iterator.next();
            BitSet lhs = candidate.left();
            BitSet rhs = candidate.right();

            // Check posCover status first
            ObjectObjectImmutablePair<BitSet, BitSet> status = this.root.getRhsForLhsPaths(lhs, rhs);
            BitSet confirmedRhs = status.left();
            BitSet remainingRhs = status.right();

            if (rhs.cardinality() > confirmedRhs.cardinality() + remainingRhs.cardinality()) {
                //total bits in confirmed and remaining is less than given rhs attribute
                iterator.remove();
                continue;
            }

            if(rhs.equals(confirmedRhs)){
                confirmList.add(new ObjectObjectImmutablePair<>(lhs, rhs));
                iterator.remove();
                continue;
            }

            // Validate remaining bits directly
            ValidationTask task = new ValidationTask(lhs, remainingRhs);
            ValidationResult result = task.call();

            BitSet validRhs   = result.validRhs();
            BitSet invalidRhs = (BitSet) remainingRhs.clone();
            invalidRhs.andNot(validRhs);

            validFDCount   += validRhs.cardinality();
            invalidFDCount += invalidRhs.cardinality();

            if (!validRhs.isEmpty()) {
                this.root.markAsValidate(lhs, validRhs);
                confirmedRhs.or(validRhs);
                if(rhs.equals(confirmedRhs)){
                    confirmList.add(new ObjectObjectImmutablePair<>(lhs, rhs));
                }
            }

            iterator.remove();

            if (!invalidRhs.isEmpty()) {
                for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                    this.root.specializePositiveCover(lhs, attr);
                }

                suggestions.addAll(result.suggestions());
                if (validFDCount > 0 && invalidFDCount > validFDCount * validationThreshold) {
                    return suggestions;
                }
            }
        }

        return (pendingList.isEmpty() && suggestions.isEmpty()) ? null : suggestions;
    }

    //Induces the positive cover from a negative cover (agree-sets).
    private void inductPositiveCover(NegativeCover negCover) {
        for (int i = negCover.getLevels().size() - 1; i >= 0; i--) { //Iterate in reverse order
            for(BitSet agreeLhs : negCover.getLevels().get(i)){
                BitSet violatedRhs = (BitSet) agreeLhs.clone();
                violatedRhs.flip(0, numAttributes);

                for (int rhs = violatedRhs.nextSetBit(0); rhs >= 0; rhs = violatedRhs.nextSetBit(rhs + 1)) {
                    this.root.specializePositiveCover(agreeLhs, rhs);
                }
            }
        }

        this.isInitialValidation = false;
    }

    public static List<BitSet> minimalCrossProduct(List<BitSet> list1, List<BitSet> list2) {
        Comparator<BitSet> comparator = (a, b) -> {
            int sizeCmp = Integer.compare(a.cardinality(), b.cardinality());
            if (sizeCmp != 0) return sizeCmp;
            for (int i = a.nextSetBit(0), j = b.nextSetBit(0); i >= 0;
                 i = a.nextSetBit(i + 1), j = b.nextSetBit(j + 1)) {
                int cmp = Integer.compare(i, j);
                if (cmp != 0) return cmp;
            }
            return 0;
        };

        list1.sort(comparator);
        list2.sort(comparator);

        List<BitSet> result = new ArrayList<>();

        for (BitSet bs1 : list1) {
            for (BitSet bs2 : list2) {
                BitSet union = (BitSet) bs1.clone();
                union.or(bs2);
                addMinimal(result, union);
            }
        }

        return result;
    }

    private static void addMinimal(List<BitSet> result, BitSet candidate) {
        for (BitSet existing : result) {
            if (isSubset(existing, candidate)) return; // candidate is superset, skip
        }
        result.removeIf(existing -> isSubset(candidate, existing));
        result.add(candidate);
    }

    private static boolean isSubset(BitSet a, BitSet b) {
        BitSet temp = (BitSet) a.clone();
        temp.andNot(b);
        return temp.isEmpty();
    }
}