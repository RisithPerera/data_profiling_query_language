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
                        Int2IntMap rhsPreClusterIds = seen.get(key);
                        for (int r = validRhs.nextSetBit(0); r >= 0; r = validRhs.nextSetBit(r + 1)) {
                            if (FDValidator.this.compressed[record][r] == -1 || FDValidator.this.compressed[record][r] != rhsPreClusterIds.get(r)) {
                                suggestions.add(new IntIntImmutablePair(record, representative.getInt(key)));
                                validRhs.clear(r);
                                if (validRhs.isEmpty()) {
                                    return new ValidationResult(lhs, rhs, validRhs, suggestions);
                                }
                            }
                        }
                    } else {
                        Int2IntMap rhsClusterIds = new Int2IntOpenHashMap();
                        for (int rhsAttr = validRhs.nextSetBit(0); rhsAttr >= 0; rhsAttr = validRhs.nextSetBit(rhsAttr + 1)) {
                            rhsClusterIds.put(rhsAttr, FDValidator.this.compressed[record][rhsAttr]);
                        }
                        seen.put(key, rhsClusterIds);
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

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount = 0;
        int invalidFDCount = 0;

        // Validate until no more possible candidates at size <= level
        while (true) {
            Map<BitSet, BitSet> candidates = this.root.getUnvalidatedCandidatesUpToDepth(level);
            if (candidates.isEmpty()) break;

            log.info("Level: {} Found FD candidates: {}", level, candidates.size());

            List<Future<ValidationResult>> futures = new ArrayList<>();
            for (Map.Entry<BitSet, BitSet> entry : candidates.entrySet()) {
                futures.add(executor.submit(new ValidationTask((BitSet) entry.getKey().clone(), (BitSet) entry.getValue().clone())));
            }

            boolean isSpecializedHappend = false;
            for (Future<ValidationResult> future : futures) {
                ValidationResult result = future.get();

                BitSet lhs = result.lhs();
                BitSet validRhs = result.validRhs();
                BitSet invalidRhs = (BitSet) result.rhs().clone();
                invalidRhs.andNot(validRhs);

                totalCandidateCount += result.rhs().cardinality();
                invalidFDCount += invalidRhs.cardinality();

                if (!validRhs.isEmpty()) {
                    this.root.markAsValidate(lhs, validRhs);
                }

                if (!invalidRhs.isEmpty()) {
                    for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                        this.root.specializePositiveCover(lhs, attr);
                    }
                    isSpecializedHappend = true;
                    suggestions.addAll(result.suggestions());
                }
            }

            if(!isSpecializedHappend){
                break; //Everything Validated! No need check again.
            }

            if (invalidFDCount > totalCandidateCount * validationThreshold) {
                log.debug("Returning to sampling. IC:{} TC:{}", invalidFDCount, totalCandidateCount);
                return suggestions;
            }
        }

        // Collect all validated FDs on the given level size
        this.root.getValidatedFDsAtDepth(level, results);

        return null;
    }

    public Set<IntIntImmutablePair> validateFreeLock(ExecutorService executor,
                                                     NegativeCover newNegativeCover,
                                                     Set<BitSet> rhsCombinations,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount = 0;
        int invalidFDCount = 0;

        Iterator<BitSet> rhsCombinationIterator = rhsCombinations.iterator();

        while (rhsCombinationIterator.hasNext()) {
            BitSet candidateRhs = rhsCombinationIterator.next();
            BitSet targetRhs = (BitSet) candidateRhs.clone();

            // Validate until no more possible candidates at size <= level
            while (!targetRhs.isEmpty()) {
                Map<BitSet, BitSet> candidates = this.root.getUnvalidatedCandidatesForRhs(targetRhs);
                if (candidates.isEmpty()) break;

                log.info("Target Rhs: {} Found FD candidates: {}", targetRhs, candidates.size());

                List<Future<ValidationResult>> futures = new ArrayList<>();
                for (Map.Entry<BitSet, BitSet> entry : candidates.entrySet()) {
                    futures.add(executor.submit(new ValidationTask((BitSet) entry.getKey().clone(), (BitSet) entry.getValue().clone())));
                }

                boolean isSpecializedHappend = false;
                for (Future<ValidationResult> future : futures) {
                    ValidationResult result = future.get();

                    BitSet lhs = result.lhs();
                    BitSet validRhs = result.validRhs();
                    BitSet invalidRhs = (BitSet) result.rhs().clone();
                    invalidRhs.andNot(validRhs);

                    totalCandidateCount += result.rhs().cardinality();
                    invalidFDCount += invalidRhs.cardinality();

                    if (!validRhs.isEmpty()) {
                        this.root.markAsValidate(lhs, validRhs);
                    }

                    if (!invalidRhs.isEmpty()) {
                        for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                            this.root.specializePositiveCover(lhs, attr);
                        }
                        isSpecializedHappend = true;
                        suggestions.addAll(result.suggestions());
                    }
                }

                if(!isSpecializedHappend){
                    break; //Everything Validated! No need check again.
                }

                if (invalidFDCount > totalCandidateCount * validationThreshold) {
                    log.debug("Returning to sampling. IC:{} TC:{}", invalidFDCount, totalCandidateCount);
                    return suggestions;
                }
            }

            log.debug("Target Rhs: {} validated all candidates", candidateRhs);

            List<BitSet> result = null;

            for (int rhsAttr = candidateRhs.nextSetBit(0); rhsAttr >= 0; rhsAttr = candidateRhs.nextSetBit(rhsAttr + 1)) {
                List<BitSet> bitPaths = this.root.getValidatedLhsPathsForRhs(rhsAttr, candidateRhs);
                if (result == null) {
                    result = new ArrayList<>(bitPaths);
                } else {
                    result = minimalCrossProduct(result, bitPaths);
                }
            }

            if (result != null) {
                for (BitSet mergedLhs : result) {
                    confirmList.add(new ObjectObjectImmutablePair<>(mergedLhs, candidateRhs));
                }
            }

            rhsCombinationIterator.remove();
        }

        return null;
    }

    public Set<IntIntImmutablePair> validateLockFree(NegativeCover newNegativeCover,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> pendingList,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) {

        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount   = 0;
        int invalidFDCount = 0;

        Iterator<ObjectObjectImmutablePair<BitSet, BitSet>> iterator = pendingList.iterator();

        while (iterator.hasNext()) {
            ObjectObjectImmutablePair<BitSet, BitSet> candidate = iterator.next();
            BitSet lhs = candidate.left();
            BitSet rhs = candidate.right();

            ObjectObjectImmutablePair<BitSet, BitSet> status = this.root.getRhsCompositionForLhs(lhs, rhs);
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

            totalCandidateCount += remainingRhs.cardinality();
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

                if (invalidFDCount > totalCandidateCount * validationThreshold) {
                    log.debug("Returning to sampling. IC:{} TC:{}", invalidFDCount, totalCandidateCount);
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
        int totalCandidateCount   = 0;
        int invalidFDCount = 0;

        Iterator<ObjectObjectImmutablePair<BitSet, BitSet>> iterator = pendingList.iterator();

        while (iterator.hasNext()) {
            ObjectObjectImmutablePair<BitSet, BitSet> candidate = iterator.next();
            BitSet lhs = candidate.left();
            BitSet rhs = candidate.right();

            // Check posCover status first
            ObjectObjectImmutablePair<BitSet, BitSet> status = this.root.getRhsCompositionForLhs(lhs, rhs);
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

            totalCandidateCount += remainingRhs.cardinality();
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

                if (invalidFDCount > totalCandidateCount * validationThreshold) {
                    log.debug("Returning to sampling. IC:{} TC:{}", invalidFDCount, totalCandidateCount);
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

    private List<BitSet> minimalCrossProduct(List<BitSet> list1, List<BitSet> list2) {
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

    private void addMinimal(List<BitSet> result, BitSet candidate) {
        for (BitSet existing : result) {
            if (isSubset(existing, candidate)) return; // candidate is superset, skip
        }
        result.removeIf(existing -> isSubset(candidate, existing));
        result.add(candidate);
    }

    private boolean isSubset(BitSet a, BitSet b) {
        BitSet temp = (BitSet) a.clone();
        temp.andNot(b);
        return temp.isEmpty();
    }
}