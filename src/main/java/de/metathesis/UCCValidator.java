package de.metathesis;

import de.metanome.algorithms.hyucc.structures.ClusterIdentifier;
import de.metanome.algorithms.hyucc.structures.IntegerPair;
import de.metathesis.structures.NegativeCover;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import de.metathesis.structures.UCCTreeNode;
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

public class UCCValidator {
    private static final Logger log = LogManager.getLogger(UCCValidator.class);

    @Getter
    private final int numAttributes;
    private final int[][] compressed;
    private final PositionListIndex[] plis;

    private final double validationThreshold = 0.01;

    private final UCCTreeNode root;

    public UCCValidator(Relation relation) {
        this.numAttributes = relation.getNumOfAttributes();
        this.compressed = relation.getCompressedRecords();
        this.plis = relation.getUnaryPLIs();

        this.root = new UCCTreeNode(numAttributes);
        this.root.setChildren(new UCCTreeNode[numAttributes]);

        //Initialize Most General Uniques
        for (int attr = 0; attr < this.numAttributes; attr++) {
            this.root.getChildren()[attr] = new UCCTreeNode(this.numAttributes, true);
        }
    }

    private class ValidationTask implements Callable<ValidationResult> {
        private final BitSet ucc;


        public ValidationTask(BitSet ucc) {
            this.ucc = ucc;
        }

        public ValidationResult call() {
            // Check if Size 1 UCC is a unique
            if (ucc.isEmpty()) {
                return new ValidationResult(ucc, false, Collections.emptySet());
            }

            if (ucc.cardinality() == 1) {
                int uccAttr = ucc.nextSetBit(0);
                return new ValidationResult(ucc, UCCValidator.this.plis[uccAttr].isUnique(), Collections.emptySet());
            }

            Set<IntIntImmutablePair> suggestions = new HashSet<>();

            int firstUccAttribute = ucc.nextSetBit(0);
            BitSet remainingUcc = (BitSet) ucc.clone();
            remainingUcc.clear(firstUccAttribute);

            for (IntArrayList cluster : UCCValidator.this.plis[firstUccAttribute].getClusters()) {
                Object2IntMap<IntArrayList> value2record = new Object2IntOpenHashMap<>();
                for (int recordId : cluster) {
                    IntArrayList key = Utility.buildKey(remainingUcc, UCCValidator.this.compressed[recordId]);
                    if (key == null) {
                        continue;
                    }

                    if (value2record.containsKey(key)) {
                        suggestions.add(new IntIntImmutablePair(recordId, value2record.getInt(key)));
                        return new ValidationResult(ucc, false, suggestions);
                    }
                    value2record.put(key, recordId);
                }
            }

            return new ValidationResult(ucc, true, suggestions);
        }
    }

    private record ValidationResult(
            BitSet ucc,
            boolean isValidUCC,
            Set<IntIntImmutablePair> suggestions) {
    }

    // Validate using positive cover induction
    public Set<IntIntImmutablePair> validateWithPositiveCover(ExecutorService executor,
                                                              NegativeCover newNegativeCover,
                                                              int level) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Set<BitSet> candidatesTemp = this.root.getLevel(level);

        List<Future<ValidationResult>> futures = new ArrayList<>(candidatesTemp.size());

        for (BitSet candidate : candidatesTemp) {
            futures.add(executor.submit(new ValidationTask(candidate)));
        }

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validUCCCount   = 0;
        int invalidUCCCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            ValidationResult result = futures.get(i).get();

            // Valid RHS bits — confirm in posCover and store results
            if (result.isValidUCC()) {
                validUCCCount++;
                this.root.markAsValidate(result.ucc());
            } else {
                invalidUCCCount++;
                specializePositiveCover(result.ucc());
                suggestions.addAll(result.suggestions());

                if (validUCCCount > 0 && invalidUCCCount > validUCCCount * validationThreshold) {
                    // Cancel remaining queued tasks already
                    log.info("Cancelling Pending Tasks. VC: {}, IC: {}, VE: {}", validUCCCount, invalidUCCCount, validUCCCount * validationThreshold);

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

//    public Set<IntIntImmutablePair> validateLockedCandidates(NegativeCover newNegativeCover,
//                                                             List<ObjectObjectImmutablePair<BitSet, BitSet>> pendingList,
//                                                             List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmList) {
//
//        inductPositiveCover(newNegativeCover);
//
//        Set<IntIntImmutablePair> suggestions = new HashSet<>();
//        int validFDCount   = 0;
//        int invalidFDCount = 0;
//
//        Iterator<ObjectObjectImmutablePair<BitSet, BitSet>> iterator = pendingList.iterator();
//
//        while (iterator.hasNext()) {
//            ObjectObjectImmutablePair<BitSet, BitSet> candidate = iterator.next();
//            BitSet lhs = candidate.left();
//            BitSet rhs = candidate.right();
//
//            // Check posCover status first
//            ObjectObjectImmutablePair<BitSet, BitSet> status = checkStatus(lhs, rhs);
//            BitSet confirmedRhs = status.left();
//            BitSet remainingRhs = status.right();
//
//            if (confirmedRhs.isEmpty() && remainingRhs.isEmpty()) {
//                //Nothing is confirmed and Nothing is remaining to confirm
//                iterator.remove();
//                continue;
//            }
//
//            if (remainingRhs.isEmpty()) {
//                confirmList.add(new ObjectObjectImmutablePair<>(lhs, confirmedRhs));
//                iterator.remove();
//                continue;
//            }
//
//            // Validate remaining bits directly
//            ValidationTask task = new ValidationTask(lhs, remainingRhs);
//            ValidationResult result = task.call();
//
//            BitSet validRhs   = result.validRhs();
//            BitSet invalidRhs = (BitSet) remainingRhs.clone();
//            invalidRhs.andNot(validRhs);
//
//            validFDCount   += validRhs.cardinality();
//            invalidFDCount += invalidRhs.cardinality();
//
//            if (!validRhs.isEmpty()) {
//                this.root.markAsValidate(lhs, validRhs);
//                confirmedRhs.or(validRhs);
//            }
//
//            confirmList.add(new ObjectObjectImmutablePair<>(lhs, confirmedRhs));
//            iterator.remove();
//
//            if (!invalidRhs.isEmpty()) {
//                for (int attr = invalidRhs.nextSetBit(0); attr >= 0; attr = invalidRhs.nextSetBit(attr + 1)) {
//                    specializePositiveCover(lhs, attr);
//                }
//
//                suggestions.addAll(result.suggestions());
//
//                if (validFDCount > 0 && invalidFDCount > validFDCount * validationThreshold) {
//                    return suggestions;
//                }
//            }
//        }
//
//        return (pendingList.isEmpty() && suggestions.isEmpty()) ? null : suggestions;
//    }

    public Set<BitSet> collectResults(int level) {
        Set<BitSet> results = new HashSet<>();
        collectValidatedAtDepth(this.root, new BitSet(), 0, level, results);
        return results;
    }

    private void collectValidatedAtDepth(UCCTreeNode node, BitSet currentUCC, int depth, int targetDepth, Set<BitSet> results) {
        if (node == null){
            return;
        }

        if (depth == targetDepth) {
            if(node.isValidatedUCC()){
                results.add((BitSet) currentUCC.clone());
            }

            return;
        }

        for (int attr = 0; attr < numAttributes; attr++) {
            if (node.getChildren() != null && node.getChildren()[attr] != null) {
                currentUCC.set(attr);
                collectValidatedAtDepth(node.getChildren()[attr], currentUCC, depth + 1, targetDepth, results);
                currentUCC.clear(attr);
            }
        }
    }

    //Induces the positive cover from a negative cover (agree-sets).
    public void inductPositiveCover(NegativeCover nonUCCs) {
        for (int i = nonUCCs.getLevels().size() - 1; i >= 0; i--) {
            if (i >= nonUCCs.getLevels().size()) { // If this level has been trimmed during iteration
                continue;
            }

            for (BitSet nonUCC : nonUCCs.getLevels().get(i)) {
                this.specializePositiveCover(nonUCC);
            }
        }
    }

    // Specializes the positive cover for the non-UCC: agreeSet.
    private void specializePositiveCover(BitSet nonUCC) {
        List<BitSet> specUCCs = this.root.getUCCAndGeneralizations(nonUCC);

        for (BitSet specUCC : specUCCs) {
            this.root.removeUniqueColumnCombination(specUCC);

            for (int attr = this.numAttributes - 1; attr >= 0; attr--) {
                if (!nonUCC.get(attr)) {
                    specUCC.set(attr);
                    if (!this.root.containsUCCOrGeneralization(specUCC)) {
                        this.root.addUniqueColumnCombination(specUCC);
                    }
                    specUCC.clear(attr);
                }
            }
        }
    }


//    private Map<BitSet, BitSet> specializeNode(BitSet currentLhs, int additionalDepth) {
//        Map<BitSet, BitSet> result = new HashMap<>();
//
//        // Remaining attributes = all - currentLhs
//        BitSet remaining = new BitSet(numAttributes);
//        remaining.set(0, numAttributes);
//        remaining.andNot(currentLhs);
//
//        BitSet[] combinations = Utility.generateApriori(remaining, additionalDepth);
//
//        for (BitSet combo : combinations) {
//            BitSet newLhs = (BitSet) currentLhs.clone();
//            newLhs.or(combo);
//
//            BitSet newRhs = (BitSet) rhsCandidates.clone();
//            newRhs.andNot(newLhs); // non-triviality — minus entire new lhs
//
//            if (!newRhs.isEmpty()) {
//                addCandidate(result, newLhs, newRhs);
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * Returns null if any rhs bit is missing from rhsAttributes along the path — not valid.
//     * Returns a BitSet of rhs bits confirmed via rhsValidatedFds along the path.
//     */
//    private ObjectObjectImmutablePair<BitSet, BitSet> checkStatus(BitSet lhs, BitSet rhs) {
//        BitSet remaining = (BitSet) rhs.clone();
//
//        FDTreeNode current = root;
//
//        // Narrow by each node's rhsAttributes as we walk down
//        remaining.and(current.getRhsAttributes());
//
//        BitSet confirmed = (BitSet) remaining.clone();
//        confirmed.and(current.getRhsValidatedFds());
//        remaining.andNot(confirmed);
//
//        if (remaining.isEmpty()) return new ObjectObjectImmutablePair<>(confirmed, remaining);
//
//        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
//
//            if (current.getChildren() == null || current.getChildren()[attr] == null) {
//                remaining.and(current.getRhsCandidateFds());
//                return new ObjectObjectImmutablePair<>(confirmed, remaining);
//            }
//
//            current = current.getChildren()[attr];
//
//            remaining.and(current.getRhsAttributes());
//            if (remaining.isEmpty()) return new ObjectObjectImmutablePair<>(confirmed, remaining);
//
//            BitSet newlyConfirmed = (BitSet) remaining.clone();
//            newlyConfirmed.and(current.getRhsValidatedFds());
//            confirmed.or(newlyConfirmed);
//            remaining.andNot(newlyConfirmed);
//
//            if (remaining.isEmpty()) return new ObjectObjectImmutablePair<>(confirmed, remaining);
//        }
//
//        remaining.and(current.getRhsCandidateFds());
//        return new ObjectObjectImmutablePair<>(confirmed, remaining);
//    }
//
    private void addCandidate(Map<BitSet, BitSet> map, BitSet lhs, BitSet rhs) {
        map.merge(lhs, rhs, (existing, newRhs) -> {
            existing.or(newRhs);
            return existing;
        });
    }
}