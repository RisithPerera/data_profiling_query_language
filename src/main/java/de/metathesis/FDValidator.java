package de.metathesis;

import de.metathesis.structures.FDTreeNode;
import de.metathesis.structures.NegativeCover;
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
            Set<IntIntImmutablePair> suggestions) {
    }

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
                specializePositiveCover(lhs, attr);
            }

            if (!invalidRhs.isEmpty()) {
                suggestions.addAll(result.suggestions());
            }
        }

//        if (invalidFDCount > totalCandidateCount * validationThreshold) {
//            log.info("Back to Sampling | TC: {}, IC: {}, VE: {}", totalCandidateCount, invalidFDCount, totalCandidateCount * validationThreshold);
//            return suggestions;
//        }

        return null;
    }

    public Set<IntIntImmutablePair> validateFreeLock(ExecutorService executor,
                                                     NegativeCover newNegativeCover,
                                                     Set<BitSet> rhsCandidateList,
                                                     List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validFDCount   = 0;
        int invalidFDCount = 0;

        Iterator<BitSet> iterator = rhsCandidateList.iterator();

        while (iterator.hasNext()) {
            BitSet rhsCandidate = iterator.next();
            List<FDTreeNode.RhsSearchResult> candidates = this.root.findAllLhsForRhs(rhsCandidate);

            for(FDTreeNode.RhsSearchResult candidate : candidates){
                BitSet lhsCandidate = candidate.lhs();
                BitSet confirmedRhs = candidate.confirmed();
                BitSet remainingRhs = candidate.remaining();

                if(rhsCandidate.equals(confirmedRhs)){
                    confirmList.add(new ObjectObjectImmutablePair<>(lhsCandidate, rhsCandidate));
                    continue;
                }

                // Validate remaining bits directly
                ValidationTask task = new ValidationTask(lhsCandidate, remainingRhs);
                ValidationResult result = task.call();

                BitSet validRhs   = result.validRhs();
                BitSet invalidRhs = (BitSet) remainingRhs.clone();
                invalidRhs.andNot(validRhs);

                validFDCount   += validRhs.cardinality();
                invalidFDCount += invalidRhs.cardinality();

                if (!validRhs.isEmpty()) {
                    this.root.markAsValidate(lhsCandidate, validRhs);
                    confirmedRhs.or(validRhs);

                    if(rhsCandidate.equals(confirmedRhs)){
                        confirmList.add(new ObjectObjectImmutablePair<>(lhsCandidate, rhsCandidate));
                        continue;
                    }
                }

                if (!invalidRhs.isEmpty()) {
                    for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                        specializePositiveCover(lhsCandidate, attr);
                    }

                    suggestions.addAll(result.suggestions());
                }
            }

            iterator.remove();

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

            // Check posCover status first
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
                    specializePositiveCover(lhs, attr);
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
    public void inductPositiveCover(NegativeCover negCover) {
        for (int i = negCover.getLevels().size() - 1; i >= 0; i--) { //Iterate in reverse order
            for(BitSet agreeLhs : negCover.getLevels().get(i)){
                BitSet violatedRhs = (BitSet) agreeLhs.clone();
                violatedRhs.flip(0, numAttributes);

                for (int rhs = violatedRhs.nextSetBit(0); rhs >= 0; rhs = violatedRhs.nextSetBit(rhs + 1)) {
                    specializePositiveCover(agreeLhs, rhs);
                }
            }
        }

        this.isInitialValidation = false;
    }

    // Specializes the positive cover for the non FD: agreeSet /-> rhs.
    private void specializePositiveCover(BitSet lhs, int rhs) {
        List<BitSet> specLhss = this.root.getFdAndGeneralizations(lhs, rhs);

        for (BitSet specLhs : specLhss) {
            this.root.removeFunctionalDependency(specLhs, rhs);

            for (int attr = this.numAttributes - 1; attr >= 0; attr--) { // TODO: Is iterating backwards a good or bad idea?
                if (!lhs.get(attr) && (attr != rhs)) {
                    specLhs.set(attr);
                    if (!this.root.containsFdOrGeneralization(specLhs, rhs)) {
                        this.root.addFunctionalDependency(specLhs, rhs);
                    }
                    specLhs.clear(attr);
                }
            }
        }
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
}