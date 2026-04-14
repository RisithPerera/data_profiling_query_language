package de.metathesis;

import de.metathesis.structures.NegativeCover;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.PositiveCoverNode;
import de.metathesis.structures.Relation;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.NonNull;

import javax.validation.Validation;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FDValidator {
    @Getter
    private final int numAttributes;
    private final int[][] compressed;
    private final PositionListIndex[] plis;

    private final List<List<ObjectOpenHashSet<BitSet>>> validFDs = new ArrayList<>();
    private final double validationThreshold = 0.01;

    private final PositiveCoverNode root;

    // Tracks the deepest level that has been fully specialized so far.
    // Starts at 0 (only [] -> all exists). Incremented lazily by
    // getCandidatesAtLevel when the caller requests a deeper level.
    private int currentDepth;

    public FDValidator(Relation relation) {
        this.numAttributes = relation.getNumOfAttributes();
        this.compressed = relation.getCompressedRecords();
        this.plis = relation.getUnaryPLIs();

        this.root = new PositiveCoverNode(numAttributes);
        //Initialize Add Most General Dependencies
        this.root.getRhsCandidateFds().set(0, numAttributes);
        this.root.getRhsAttributes().set(0, numAttributes);
        this.currentDepth = 0;

        // Initialize validFDs — outer list by rhs attribute, inner list by lhs size
        for (int i = 0; i < numAttributes; i++) {
            List<ObjectOpenHashSet<BitSet>> byLevel = new ArrayList<>();
            for (int j = 0; j <= numAttributes; j++) {
                byLevel.add(new ObjectOpenHashSet<>());
            }
            validFDs.add(byLevel);
        }

        for (int rhsAttribute = 0; rhsAttribute < numAttributes; rhsAttribute++) {
            if (plis[rhsAttribute].isConstant()) {
                validFDs.get(rhsAttribute).getFirst().add(new BitSet(numAttributes));
            }
        }
    }

    //This method is validating without using positive cover
    public Map<BitSet, List<BitSet>> validateDirectly(ExecutorService executor, int level) throws ExecutionException, InterruptedException {
        Map<BitSet, List<BitSet>> foundFds = new HashMap<>();

//        //Handle level zero implicitly
//        if(level == 0){
//            for (int rhsAttribute = 0; rhsAttribute < numAttributes; rhsAttribute++) {
//                if (plis[rhsAttribute].isConstant()) {
//                    BitSet rhs = new BitSet(this.numAttributes);
//                    rhs.set(rhsAttribute);
//                    foundFds.computeIfAbsent(rhs, k -> new ArrayList<>()).add(new BitSet());
//                }
//            }
//
//            return foundFds;
//        }

        //Generate all the level candidates
        BitSet[] levelCandidates = Utility.generateApriori(this.numAttributes, level);
        List<Future<ValidationResult>> levelFutures = new ArrayList<>();

        for (BitSet lhs : levelCandidates) {
            // Start with all non-LHS attributes as RHS candidates
            BitSet rhs = new BitSet(numAttributes);
            rhs.set(0, numAttributes);
            rhs.andNot(lhs); // remove LHS attributes

            // Remove non-minimal RHS attributes
            for (int rhsAttribute = rhs.nextSetBit(0); rhsAttribute >= 0; rhsAttribute = rhs.nextSetBit(rhsAttribute + 1)) {
                if (checkMinimality(lhs, rhsAttribute)) {
                    rhs.clear(rhsAttribute);
                }
            }

            if (rhs.isEmpty()) {
                continue;
            }

            levelFutures.add(executor.submit(new ValidationTask((BitSet) lhs.clone(), rhs)));
        }

        for (Future<ValidationResult> future : levelFutures) {
            ValidationResult result = future.get();

            // Each set bit in validRhs is a separate valid FD
            for (int rhsAttr = result.getValidRhs().nextSetBit(0); rhsAttr >= 0; rhsAttr = result.getValidRhs().nextSetBit(rhsAttr + 1)) {

                synchronized (validFDs.get(rhsAttr).get(level)) {
                    validFDs.get(rhsAttr).get(level).add((BitSet) result.getLhs().clone());
                }

                BitSet rhsBitSet = new BitSet(numAttributes);
                rhsBitSet.set(rhsAttr);
                foundFds.computeIfAbsent(rhsBitSet, k -> new ArrayList<>()).add((BitSet) result.getLhs().clone());
            }
        }

        return foundFds;
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
                    IntArrayList key = buildKey(remainingLhs, record, FDValidator.this.compressed);
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

        private IntArrayList buildKey(BitSet remainingLhs, int rec, int[][] compressedRecords) {
            IntArrayList key = new IntArrayList();
            for (int attr = remainingLhs.nextSetBit(0); attr >= 0; attr = remainingLhs.nextSetBit(attr + 1)) {
                int v = compressedRecords[rec][attr];
                if (v == -1) {
                    return null;
                }
                key.add(v);
            }
            return key;
        }
    }

    @Getter
    private class ValidationResult {
        private final BitSet lhs;
        private final BitSet rhs;
        private final BitSet validRhs;
        private final Set<IntIntImmutablePair> suggestions;

        public ValidationResult(BitSet lhs, BitSet rhs, BitSet validRhs, Set<IntIntImmutablePair> suggestions) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.validRhs = validRhs;
            this.suggestions = suggestions;
        }
    }

    private boolean checkMinimality(BitSet lhs, int rhs){
        // Minimality check — previous levels already complete
        for (int previousLevel = 0; previousLevel < lhs.cardinality(); previousLevel++) {
            for (BitSet validLhs : validFDs.get(rhs).get(previousLevel)) {
                BitSet tmp = (BitSet) validLhs.clone();
                tmp.andNot(lhs);
                if (tmp.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    // Validate using positive cover induction
    public Set<IntIntImmutablePair> validateWithPositiveCover(ExecutorService executor,
                                                              NegativeCover newNegativeCover,
                                                              int level) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Map<BitSet, BitSet> candidatesTemp = getCandidatesAtDepth(level);

        List<Future<ValidationResult>> futures = new ArrayList<>(candidatesTemp.size());

        for (Map.Entry<BitSet, BitSet> candidate : candidatesTemp.entrySet()) {
            futures.add(executor.submit(new ValidationTask((BitSet) candidate.getKey().clone(), (BitSet) candidate.getValue().clone())));
        }

//        List<CandidateFD> candidates = getCandidatesAtLevel(level);
//        if (candidates.isEmpty()){
//            return null;
//        }
//
//        List<Future<ValidationResult>> futures = new ArrayList<>(candidates.size());
//
//        for (CandidateFD candidate : candidates) {
//            futures.add(executor.submit(new ValidationTask((BitSet) candidate.lhs.clone(), (BitSet) candidate.rhsCandidates.clone())));
//        }

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validFDCount   = 0;
        int invalidFDCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            ValidationResult result = futures.get(i).get();

            BitSet lhs      = result.getLhs();
            BitSet rhs      = result.getRhs();
            BitSet validRhs = result.getValidRhs();
            validFDCount += validRhs.cardinality();

            // Invalid RHS bits — specialize posCover
            BitSet invalidRhs = (BitSet) rhs.clone();
            invalidRhs.andNot(validRhs);
            invalidFDCount += invalidRhs.cardinality();

            // Valid RHS bits — confirm in posCover and store results
            if (!validRhs.isEmpty()) {
                this.root.markAsValidate(lhs, validRhs);
            }

            for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
                specializePositiveCover(lhs, attr);
            }

            if (!invalidRhs.isEmpty()) {
                suggestions.addAll(result.getSuggestions());

                if (validFDCount > 0 && invalidFDCount > validFDCount * validationThreshold) {
                    // Cancel remaining queued tasks already
                    System.out.printf("Cancelling Pending Tasks. VC: %d, IC: %d, VE: %.2f\n", validFDCount, invalidFDCount, validFDCount * validationThreshold);

                    for (int j = i + 1; j < futures.size(); j++) {
                        futures.get(j).cancel(true);
                    }
                    return suggestions;
                }
            }
        }

        // All candidates checked without hitting threshold.
        return suggestions.isEmpty() ? null : suggestions;
    }

    public Set<IntIntImmutablePair> validateLockedCandidates(NegativeCover newNegativeCover,
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
            ObjectObjectImmutablePair<BitSet, BitSet> status = checkStatus(lhs, rhs);
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

            BitSet validRhs   = result.getValidRhs();
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

                suggestions.addAll(result.getSuggestions());

                if (validFDCount > 0 && invalidFDCount > validFDCount * validationThreshold) {
                    return suggestions;
                }
            }
        }

        return (pendingList.isEmpty() && suggestions.isEmpty()) ? null : suggestions;
    }

    public Map<BitSet, List<BitSet>> collectResults(int level) {
        Map<BitSet, List<BitSet>> results = new HashMap<>();
        collectValidatedAtDepth(root, new BitSet(), 0, level, results);
        return results;
    }

    private void collectValidatedAtDepth(PositiveCoverNode node, BitSet currentLhs, int depth, int targetDepth, Map<BitSet, List<BitSet>> results) {

        if (node == null || node.isEmpty()) return;

        if (depth == targetDepth) {
            for (int attr = node.getRhsValidatedFds().nextSetBit(0); attr >= 0; attr = node.getRhsValidatedFds().nextSetBit(attr + 1)) {
                BitSet rhs = new BitSet(numAttributes);
                rhs.set(attr);
                results.computeIfAbsent(rhs, k -> new ArrayList<>()).add((BitSet) currentLhs.clone());
            }
            return;
        }

        for (int attr = 0; attr < numAttributes; attr++) {
            if (node.getChildren() != null && node.getChildren()[attr] != null) {
                currentLhs.set(attr);
                collectValidatedAtDepth(node.getChildren()[attr], currentLhs, depth + 1, targetDepth, results);
                currentLhs.clear(attr);
            }
        }
    }

    //Induces the positive cover from a negative cover (agree-sets).
    public void inductPositiveCover(NegativeCover negCover) {
        for (int i = negCover.getFdLevels().size() - 1; i >= 0; i--) { //Iterate in reverse order
            for(BitSet agreeLhs : negCover.getFdLevels().get(i)){
                BitSet violatedRhs = (BitSet) agreeLhs.clone();
                violatedRhs.flip(0, numAttributes);

                for (int rhs = violatedRhs.nextSetBit(0); rhs >= 0; rhs = violatedRhs.nextSetBit(rhs + 1)) {
                    specializePositiveCover(agreeLhs, rhs);
                }
            }
        }
    }

    // Specializes the positive cover for the non-FD: agreeSet /-> rhs.
    protected void specializePositiveCover(BitSet lhs, int rhs) {
        List<BitSet> specLhss = this.root.getFdAndGeneralizations(lhs, rhs);

        if (!specLhss.isEmpty()) { // TODO: May be "while" instead of "if"?
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
    }

    private Map<BitSet, BitSet> getCandidatesAtDepth(int targetDepth) {
        Map<BitSet, BitSet> candidates = new HashMap<>();
        // Start with unvalidated RHS from root (depth 0)
        BitSet inheritedRhs = (BitSet) root.getRhsCandidateFds().clone();
        inheritedRhs.andNot(root.getRhsValidatedFds());
        collectAtDepth(root, new BitSet(), inheritedRhs, 0, targetDepth, candidates);
        return candidates;
    }

    private void collectAtDepth(PositiveCoverNode node, BitSet currentLhs, BitSet inheritedRhs, int depth, int targetDepth, Map<BitSet, BitSet> candidates) {

        if (depth == targetDepth) {
            // Merge node's own unvalidated candidates with inherited
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
        // current node's unvalidated candidates pass down
        BitSet parentInherited = (BitSet) node.getRhsCandidateFds().clone();
        parentInherited.andNot(node.getRhsValidatedFds());
        parentInherited.or(inheritedRhs);

        BitSet parentNotValidated = (BitSet) node.getRhsAttributes().clone();
        parentNotValidated.andNot(node.getRhsValidatedFds());

        // Follow existing tree children
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

                    //if (!parentInherited.isEmpty()) {
                    //    if (depth + 1 == targetDepth) {
                    //        // Emit directly — exactly at target depth
                    //        BitSet lhs = (BitSet) currentLhs.clone();
                    //        lhs.set(attr);
                    //        BitSet rhs = (BitSet) parentInherited.clone();
                    //        rhs.clear(attr);
                    //        if (!rhs.isEmpty()) {
                    //            addCandidate(candidates, lhs, rhs);
                    //        }
                    //    } else {
                    //        // Need to go deeper — virtually extend from this missing node
                    //        BitSet lhs = (BitSet) currentLhs.clone();
                    //        lhs.set(attr);
                    //        BitSet rhs = (BitSet) parentInherited.clone();
                    //        rhs.clear(attr);
                    //
                    //        int additionalDepth = targetDepth - depth;
                    //        Map<BitSet, BitSet> specializedCandidates = specializeNode(lhs, rhs, additionalDepth);
                    //        for (Map.Entry<BitSet, BitSet> entry : specializedCandidates.entrySet()) {
                    //            addCandidate(candidates, entry.getKey(), entry.getValue());
                    //        }
                    //    }
                    //}
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

    /**
     * Returns null if any rhs bit is missing from rhsAttributes along the path — not valid.
     * Returns a BitSet of rhs bits confirmed via rhsValidatedFds along the path.
     */
    private ObjectObjectImmutablePair<BitSet, BitSet> checkStatus(BitSet lhs, BitSet rhs) {
        BitSet remaining = (BitSet) rhs.clone();

        PositiveCoverNode current = root;

        // Narrow by each node's rhsAttributes as we walk down
        remaining.and(current.getRhsAttributes());

        BitSet confirmed = (BitSet) remaining.clone();
        confirmed.and(current.getRhsValidatedFds());
        remaining.andNot(confirmed);

        if (remaining.isEmpty()) return new ObjectObjectImmutablePair<>(confirmed, remaining);

        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {

            if (current.getChildren() == null || current.getChildren()[attr] == null) {
                remaining.and(current.getRhsCandidateFds());
                return new ObjectObjectImmutablePair<>(confirmed, remaining);
            }

            current = current.getChildren()[attr];

            remaining.and(current.getRhsAttributes());
            if (remaining.isEmpty()) return new ObjectObjectImmutablePair<>(confirmed, remaining);

            BitSet newlyConfirmed = (BitSet) remaining.clone();
            newlyConfirmed.and(current.getRhsValidatedFds());
            confirmed.or(newlyConfirmed);
            remaining.andNot(newlyConfirmed);

            if (remaining.isEmpty()) return new ObjectObjectImmutablePair<>(confirmed, remaining);
        }

        remaining.and(current.getRhsCandidateFds());
        return new ObjectObjectImmutablePair<>(confirmed, remaining);
    }

    private void addCandidate(Map<BitSet, BitSet> map, BitSet lhs, BitSet rhs) {
        map.merge(lhs, rhs, (existing, newRhs) -> {
            existing.or(newRhs);
            return existing;
        });
    }
}