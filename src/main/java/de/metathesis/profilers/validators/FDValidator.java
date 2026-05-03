package de.metathesis.profilers.validators;

import de.metanome.algorithms.hyfd.structures.FDTree;
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
import java.util.stream.Collectors;

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

        Map<BitSet, BitSet> candidates = getCandidatesAtDepth(level);

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

    public Set<IntIntImmutablePair> validateFreeLockNew(NegativeCover newNegativeCover,
                                                        Set<BitSet> rhsCandidateList,
                                                        List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) throws ExecutionException, InterruptedException{
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
                LinkedHashSet<ObjectObjectImmutablePair<BitSet, Boolean>> lhsCandidates = this.root.getLhsPathsForRhsNew(rhsCandidate, rhsAttr);

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

    public Set<IntIntImmutablePair> validateFreeLock(NegativeCover newNegativeCover,
                                                     Set<BitSet> rhsCandidateList,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validFDCount   = 0;
        int invalidFDCount = 0;

        Iterator<BitSet> rhsCandidateIterator = rhsCandidateList.iterator();
        //LinkedHashSet<FDTreeNode.FDSearchResult> specializedCandidates = new LinkedHashSet<>();

        while (rhsCandidateIterator.hasNext()) {
            BitSet rhsCandidate = rhsCandidateIterator.next();

            Map<Integer, List<FDTreeNode.FDSearchResult>> candidatesForSingleRhs = this.root.getLhsPathsForRhs(rhsCandidate);
            LinkedHashSet<FDTreeNode.FDSearchResult> candidates = mergeLhsPaths(candidatesForSingleRhs);

            while (!candidates.isEmpty() ) {
                FDTreeNode.FDSearchResult candidate = candidates.removeFirst();

                BitSet lhsCandidate = candidate.lhs();
                BitSet confirmedRhs = candidate.confirmed();
                BitSet remainingRhs = candidate.remaining();

                if (rhsCandidate.equals(confirmedRhs)) {
                    confirmList.add(new ObjectObjectImmutablePair<>(lhsCandidate, rhsCandidate));
                    continue;
                }

                // Validate remaining bits directly
                ValidationTask task = new ValidationTask(lhsCandidate, remainingRhs);
                ValidationResult result = task.call();

                BitSet validRhs = result.validRhs();
                BitSet invalidRhs = (BitSet) remainingRhs.clone();
                invalidRhs.andNot(validRhs);

                validFDCount += validRhs.cardinality();
                invalidFDCount += invalidRhs.cardinality();

                confirmedRhs.or(validRhs);

                if (!validRhs.isEmpty()) {
                    this.root.markAsValidate(lhsCandidate, validRhs);

                    if (rhsCandidate.equals(confirmedRhs)) {
                        System.out.println("Added: " + lhsCandidate + " | confirmed=" + confirmedRhs + " | remain=" + invalidRhs);
                        confirmList.add(new ObjectObjectImmutablePair<>(lhsCandidate, rhsCandidate));
                        continue;
                    }
                }

                if (!invalidRhs.isEmpty()) {
                    Map<Integer, BitSet> specializedBitMap = new HashMap<>();

                    for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                        BitSet specialized = this.root.specializePositiveCover(lhsCandidate, attr);
                        specialized.andNot(rhsCandidate);
                        specializedBitMap.put(attr, specialized);
                    }

                    System.out.println("Not Added: " + lhsCandidate + " | " + specializedBitMap + " | confirmed=" + confirmedRhs + " | invalid=" + invalidRhs);

                    Set<BitSet> specLhsCandidates = inferSpecializedLhsCandidates((BitSet) lhsCandidate.clone(), specializedBitMap);
                    for(BitSet specLhsCandidate : specLhsCandidates){
                        candidates.add(new FDTreeNode.FDSearchResult(specLhsCandidate, (BitSet) confirmedRhs.clone(), (BitSet) invalidRhs.clone()));
                    }

                    /*for(BitSet specLhsCandidate : specLhsCandidates){
                        // Check if any already confirmed LHS is a subset of specLhsCandidate
                        boolean dominated = confirmList.stream()
                                .anyMatch(c -> {
                                    if (!c.second().equals(rhsCandidate)) return false;
                                    BitSet copy = (BitSet) c.first().clone();
                                    copy.andNot(specLhsCandidate);
                                    return copy.isEmpty();
                                });

                        if (!dominated) {
                            specializedCandidates.add(new FDTreeNode.FDSearchResult(
                                    specLhsCandidate,
                                    (BitSet) confirmedRhs.clone(),
                                    (BitSet) invalidRhs.clone()
                            ));
                        }
                    }*/

                    suggestions.addAll(result.suggestions());
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

            ObjectObjectImmutablePair<BitSet, BitSet> status = this.root.searchByLhs(lhs, rhs);
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
            ObjectObjectImmutablePair<BitSet, BitSet> status = this.root.searchByLhs(lhs, rhs);
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

    private Map<BitSet, BitSet> getCandidatesAtDepth(int targetDepth) {
        Map<BitSet, BitSet> candidates = new HashMap<>();
        // Start with unvalidated RHS from root (depth 0)
        BitSet inheritedRhs = (BitSet) root.getRhsCandidateFds().clone();
        inheritedRhs.andNot(root.getRhsValidatedFds());
        collectAtDepth(root, new BitSet(), inheritedRhs, 0, targetDepth, candidates);
        return candidates;
    }

    private void collectAtDepth(FDTreeNode node, BitSet currentLhs, BitSet inheritedRhs, int depth, int targetDepth, Map<BitSet, BitSet> candidates) {

        if (depth == targetDepth) {
            // Merge node's own unvalidated remaining with inherited
            BitSet rhs = (BitSet) node.getRhsCandidateFds().clone();
            rhs.andNot(node.getRhsValidatedFds());
            rhs.or(inheritedRhs);
            rhs.andNot(currentLhs); // non-triviality

            if (!rhs.isEmpty()) {
                addCandidate(candidates, (BitSet) currentLhs.clone(), rhs);
            }
            return;
        }

        // Compute inherited RHS for children:
        // current node's unvalidated remaining pass down
        BitSet parentInherited = (BitSet) node.getRhsCandidateFds().clone();
        parentInherited.andNot(node.getRhsValidatedFds());
        parentInherited.or(inheritedRhs);

        BitSet parentNotValidated = (BitSet) node.getRhsAttributes().clone();
        parentNotValidated.andNot(node.getRhsValidatedFds());

        // Follow existing children
        if (node.getChildren() != null) {
            for (int attr = 0; attr < numAttributes; attr++) {
                BitSet attrInherited = (BitSet) parentInherited.clone();
                attrInherited.clear(attr);

                if(!parentNotValidated.isEmpty()){
                    if (node.getChildren()[attr] != null) {
                        currentLhs.set(attr);
                        collectAtDepth(node.getChildren()[attr], currentLhs, attrInherited, depth + 1, targetDepth, candidates);
                        currentLhs.clear(attr);
                    } else {
                        if (!parentInherited.isEmpty()) {
                            BitSet lhs = (BitSet) currentLhs.clone();
                            lhs.set(attr);
                            BitSet rhs = (BitSet) parentInherited.clone();
                            rhs.clear(attr);
                            int additionalDepth = targetDepth - (depth + 1);
                            specializeNode(lhs, rhs, additionalDepth);
                        }
                    }
                }
            }
        }

        // Cover all paths the tree doesn't have at all
        if (!parentInherited.isEmpty() && depth < targetDepth) {
            int additionalDepth = targetDepth - depth;
            Map<BitSet, BitSet> specializedCandidates = specializeNode(currentLhs, parentInherited, additionalDepth);
            for (Map.Entry<BitSet, BitSet> entry : specializedCandidates.entrySet()) {
                addCandidate(candidates, entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<BitSet, BitSet> specializeNode(BitSet currentLhs, BitSet rhsCandidates, int additionalDepth) {
        Map<BitSet, BitSet> result = new HashMap<>();

        // Remaining attributes = all - currentLhs
        BitSet remaining = new BitSet(numAttributes);
        remaining.set(0, numAttributes);
        remaining.andNot(currentLhs);

        BitSet[] combinations = Utility.generateApriori(remaining, additionalDepth);

        for (BitSet combo : combinations) {
            BitSet newLhs = (BitSet) currentLhs.clone();
            newLhs.or(combo);

            BitSet newRhs = (BitSet) rhsCandidates.clone();
            newRhs.andNot(newLhs); // non-triviality — minus entire new lhs

            if (!newRhs.isEmpty()) {
                addCandidate(result, newLhs, newRhs);
            }
        }

        return result;
    }

    private void addCandidate(Map<BitSet, BitSet> map, BitSet lhs, BitSet rhs) {
        map.merge(lhs, rhs, (existing, newRhs) -> {
            existing.or(newRhs);
            return existing;
        });
    }

    private LinkedHashSet<FDTreeNode.FDSearchResult> mergeLhsPaths(Map<Integer, List<FDTreeNode.FDSearchResult>> perBitPaths){
        Iterator<Map.Entry<Integer, List<FDTreeNode.FDSearchResult>>> it = perBitPaths.entrySet().iterator();
        Map.Entry<Integer, List<FDTreeNode.FDSearchResult>> first = it.next();

        List<Set<FDTreeNode.FDSearchResult>> result = new ArrayList<>();

        if(first.getValue().isEmpty()){
            return new LinkedHashSet<>();
        }

        for (FDTreeNode.FDSearchResult r : first.getValue()) {
            addMinimal(result, r); // initialize from first bit
        }

        while (it.hasNext()) {
            Map.Entry<Integer, List<FDTreeNode.FDSearchResult>> entry = it.next();
            List<Set<FDTreeNode.FDSearchResult>> merged = new ArrayList<>();

            for (Set<FDTreeNode.FDSearchResult> bucket : result) {
                if (bucket == null || bucket.isEmpty()){
                    continue;
                }

                for (FDTreeNode.FDSearchResult current : bucket) {
                    if(entry.getValue().isEmpty()){
                        return new LinkedHashSet<>();
                    }

                    for (FDTreeNode.FDSearchResult next : entry.getValue()) {
                        BitSet mergedLhs = (BitSet) current.lhs().clone();
                        mergedLhs.or(next.lhs());

                        BitSet mergedConfirmed = (BitSet) current.confirmed().clone();
                        mergedConfirmed.or(next.confirmed());

                        BitSet mergedRemaining = (BitSet) current.remaining().clone();
                        mergedRemaining.or(next.remaining());
                        mergedRemaining.andNot(mergedConfirmed);

                        addMinimal(merged, new FDTreeNode.FDSearchResult(mergedLhs, mergedConfirmed, mergedRemaining));
                    }
                }
            }

            result = merged;
        }

        return result.stream()
                .filter(s -> s != null && !s.isEmpty())
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void addMinimal(List<Set<FDTreeNode.FDSearchResult>> result, FDTreeNode.FDSearchResult candidate) {
        int candidateCard = candidate.lhs().cardinality();

        // Check any subset already available
        for (int i = 0; i < candidateCard && i < result.size(); i++) {
            for (FDTreeNode.FDSearchResult r : result.get(i)) {
                BitSet copy = (BitSet) r.lhs().clone();
                copy.andNot(candidate.lhs());
                if (copy.isEmpty()) return;
            }
        }

        // Check same candidate is available
        if (candidateCard < result.size()) {
            for (FDTreeNode.FDSearchResult r : result.get(candidateCard)) {
                BitSet copy = (BitSet) r.lhs().clone();
                copy.andNot(candidate.lhs());
                if (copy.isEmpty()) return;
            }
        }

        // Remove supersets available
        for (int i = candidateCard + 1; i < result.size(); i++) {
            result.get(i).removeIf(r -> {
                BitSet copy = (BitSet) candidate.lhs().clone();
                copy.andNot(r.lhs());
                return copy.isEmpty();
            });
        }

        // Add at correct index
        while (result.size() <= candidateCard) {
            result.add(new ObjectOpenHashSet<>());
        }

        result.get(candidateCard).add(candidate);
    }

    public Set<BitSet> inferSpecializedLhsCandidates(BitSet generalLhsCandidate, Map<Integer, BitSet> specializedBitMap) {
        Set<BitSet> candidates = new LinkedHashSet<>();
        if (specializedBitMap.values().stream().allMatch(BitSet::isEmpty)) return candidates;

        BitSet totalSpecBits = new BitSet();
        specializedBitMap.values().forEach(totalSpecBits::or);

        for(Map.Entry<Integer, BitSet> entry : specializedBitMap.entrySet()){
            int rhsAttr = entry.getKey();
            BitSet specialized = entry.getValue();

            BitSet missingSpecBits = (BitSet) totalSpecBits.clone();
            missingSpecBits.andNot(specialized);

            if(missingSpecBits.isEmpty()) continue;

            for (int uncommonLhsBit = missingSpecBits.nextSetBit(0); uncommonLhsBit >= 0; uncommonLhsBit = missingSpecBits.nextSetBit(uncommonLhsBit + 1)) {
                generalLhsCandidate.set(uncommonLhsBit);
                if (this.root.containsFdOrGeneralization(generalLhsCandidate, rhsAttr)) {
                    specializedBitMap.get(rhsAttr).set(uncommonLhsBit);
                }
                generalLhsCandidate.clear(uncommonLhsBit);
            }
        }

        Iterator<BitSet> iterator = specializedBitMap.values().iterator();
        BitSet common = (BitSet) iterator.next().clone();
        while (iterator.hasNext()) {
            common.and(iterator.next());
        }

        for (int commonLhsBit = common.nextSetBit(0); commonLhsBit >= 0; commonLhsBit = common.nextSetBit(commonLhsBit + 1)) {
            BitSet lhsCandidate = (BitSet) generalLhsCandidate.clone();
            lhsCandidate.set(commonLhsBit);

            System.out.println("New Common: " + lhsCandidate);
            candidates.add(lhsCandidate);
        }

        return candidates;
    }

    public LinkedHashSet<FDTreeNode.FDSearchResult> inferSpecializedLhsCandidatesOld(BitSet generalLhsCandidate, BitSet confirmedRhs, BitSet invalidRhs, Map<Integer, BitSet> specializedBitMap) {
        LinkedHashSet<FDTreeNode.FDSearchResult> candidates = new LinkedHashSet<>();
        if (specializedBitMap.values().stream().allMatch(BitSet::isEmpty)) return candidates;

        BitSet totalSpecBits = new BitSet();
        specializedBitMap.values().forEach(totalSpecBits::or);

        for(Map.Entry<Integer, BitSet> entry : specializedBitMap.entrySet()){
            int rhsAttr = entry.getKey();
            BitSet specialized = entry.getValue();

            BitSet missingSpecBits = (BitSet) totalSpecBits.clone();
            missingSpecBits.andNot(specialized);

            if(missingSpecBits.isEmpty()) continue;

            for (int uncommonLhsBit = missingSpecBits.nextSetBit(0); uncommonLhsBit >= 0; uncommonLhsBit = missingSpecBits.nextSetBit(uncommonLhsBit + 1)) {
                generalLhsCandidate.set(uncommonLhsBit);
                if (this.root.containsFdOrGeneralization(generalLhsCandidate, rhsAttr)) {
                    specializedBitMap.get(rhsAttr).set(uncommonLhsBit);
                }
                generalLhsCandidate.clear(uncommonLhsBit);
            }
        }

        Iterator<BitSet> iterator = specializedBitMap.values().iterator();
        BitSet common = (BitSet) iterator.next().clone();
        while (iterator.hasNext()) {
            common.and(iterator.next());
        }

        // Remove common bits from all sets
        for(BitSet bs : specializedBitMap.values()){
            bs.andNot(common);
        }

        for (int commonLhsBit = common.nextSetBit(0); commonLhsBit >= 0; commonLhsBit = common.nextSetBit(commonLhsBit + 1)) {
            BitSet lhsCandidate = (BitSet) generalLhsCandidate.clone();
            lhsCandidate.set(commonLhsBit);

            System.out.println("New Common: " + lhsCandidate + " | confirmed=" + confirmedRhs + " | remain=" + invalidRhs);
            candidates.add(new FDTreeNode.FDSearchResult(lhsCandidate, confirmedRhs, invalidRhs));
        }

        if (specializedBitMap.values().stream().allMatch(BitSet::isEmpty)) return candidates;

        // Extract common bits, add as candidates, remove from all sets
        //extractCommonCandidates(generalLhsCandidate, confirmedRhs, invalidRhs, specializedBitMap, candidates);

        //if (specializedBitMap.values().stream().allMatch(BitSet::isEmpty)) return candidates;
        // Cross-generalization — snapshot keys to avoid issues
//        List<Integer> rhsAttrs = new ArrayList<>(specializedBitMap.keySet());
//        for (int rhsAttr1 : rhsAttrs) {
//            BitSet specializedBitSet = (BitSet) specializedBitMap.get(rhsAttr1).clone(); // snapshot to avoid mutation issues
//            for (int rhsAttr2 : rhsAttrs) {
//                if (rhsAttr1 == rhsAttr2) continue;
//                for (int uncommonLhsBit = specializedBitSet.nextSetBit(0); uncommonLhsBit >= 0; uncommonLhsBit = specializedBitSet.nextSetBit(uncommonLhsBit + 1)) {
//                    generalLhsCandidate.set(uncommonLhsBit);
//                    if (this.root.containsFdOrGeneralization(generalLhsCandidate, rhsAttr2)) {
//                        specializedBitMap.get(rhsAttr2).set(uncommonLhsBit);
//                    }
//                    generalLhsCandidate.clear(uncommonLhsBit);
//                }
//            }
//        }

//        if(!specializedBitMap.isEmpty()) {
//            // Second round — find new common bits after cross-generalization
//            extractCommonCandidates(generalLhsCandidate, confirmedRhs, invalidRhs, specializedBitMap, candidates);
//
//            if (specializedBitMap.values().stream().allMatch(BitSet::isEmpty)) return candidates;
//
//            if(specializedBitMap.isEmpty()) {
//                return candidates;
//            }else{
//                // Cross product of remaining uncommon bits
//                extractCrossProductCandidates(generalLhsCandidate, confirmedRhs, invalidRhs, specializedBitMap, candidates);
//            }
//        }

        return candidates;
    }

    private void extractCommonCandidates(
            BitSet generalLhsCandidate, BitSet confirmedRhs, BitSet invalidRhs,
            Map<Integer, BitSet> specializedBitMap,
            LinkedHashSet<FDTreeNode.FDSearchResult> candidates) {

        Iterator<BitSet> iterator = specializedBitMap.values().iterator();
        BitSet common = (BitSet) iterator.next().clone();
        while (iterator.hasNext()) {
            common.and(iterator.next());
        }

        if (common.isEmpty()) return;

        for (int commonLhsBit = common.nextSetBit(0); commonLhsBit >= 0; commonLhsBit = common.nextSetBit(commonLhsBit + 1)) {
            BitSet lhsCandidate = (BitSet) generalLhsCandidate.clone();
            lhsCandidate.set(commonLhsBit);
            BitSet rhsCandidateTemp = new BitSet();
            rhsCandidateTemp.set(0);
            rhsCandidateTemp.set(3);
            BitSet forbidden = (BitSet) confirmedRhs.clone();
            forbidden.or(invalidRhs);
            if(rhsCandidateTemp.equals(forbidden) && !specializedBitMap.isEmpty()){
                System.out.println("New: " + lhsCandidate + " | confirmed=" + confirmedRhs + " | remain=" + invalidRhs);
            }
            candidates.add(new FDTreeNode.FDSearchResult(lhsCandidate, confirmedRhs, invalidRhs));
        }

        // Remove common bits from all sets
        for(BitSet bs : specializedBitMap.values()){
            bs.andNot(common);
        }
    }

    private void extractCrossProductCandidates(
            BitSet generalLhsCandidate, BitSet confirmedRhs, BitSet invalidRhs,
            Map<Integer, BitSet> specializedBitMap,
            LinkedHashSet<FDTreeNode.FDSearchResult> candidates) {

        // Start with a single empty extension, grow by OR-ing each rhs attr's options
        List<BitSet> extensions = new ArrayList<>();
        extensions.add(new BitSet());

        for (BitSet options : specializedBitMap.values()) {
            if (options.isEmpty()) return;
            List<BitSet> newExtensions = new ArrayList<>();
            for (BitSet existing : extensions) {
                for (int bit = options.nextSetBit(0); bit >= 0; bit = options.nextSetBit(bit + 1)) {
                    BitSet extended = (BitSet) existing.clone();
                    extended.set(bit);
                    newExtensions.add(extended);
                }
            }
            extensions = newExtensions;
        }

        for (BitSet extension : extensions) {
            if (extension.isEmpty()) continue;
            BitSet lhsCandidate = (BitSet) generalLhsCandidate.clone();
            lhsCandidate.or(extension);

            BitSet rhsCandidateTemp = new BitSet();
            rhsCandidateTemp.set(0);
            rhsCandidateTemp.set(3);
            BitSet forbidden = (BitSet) confirmedRhs.clone();
            forbidden.or(invalidRhs);
            if(rhsCandidateTemp.equals(forbidden) && !specializedBitMap.isEmpty()){
                System.out.println("New Crossed: " + lhsCandidate + " | confirmed=" + confirmedRhs + " | remain=" + invalidRhs);
            }

            candidates.add(new FDTreeNode.FDSearchResult(lhsCandidate, confirmedRhs, invalidRhs));
        }
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
                addMinimal2(result, union);
            }
        }

        return result;
    }

    private static void addMinimal2(List<BitSet> result, BitSet candidate) {
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