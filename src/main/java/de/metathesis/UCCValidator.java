package de.metathesis;

import de.metathesis.structures.NegativeCover;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import de.metathesis.structures.UCCTreeNode;
import de.metathesis.structures.UCCTreeNode.ValidationStatus;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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

    @Getter
    private boolean isInitialValidation = true;

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
                return new ValidationResult(ucc, ValidationStatus.INVALID, Collections.emptySet());
            }

            if (ucc.cardinality() == 1) {
                int uccAttr = ucc.nextSetBit(0);
                ValidationStatus status = UCCValidator.this.plis[uccAttr].isUnique() ? ValidationStatus.VALID : ValidationStatus.INVALID;
                return new ValidationResult(ucc, status, Collections.emptySet());
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
                        return new ValidationResult(ucc, ValidationStatus.INVALID, suggestions);
                    }
                    value2record.put(key, recordId);
                }
            }

            return new ValidationResult(ucc, ValidationStatus.VALID, suggestions);
        }
    }

    private record ValidationResult(
            BitSet ucc,
            ValidationStatus status,
            Set<IntIntImmutablePair> suggestions) {
    }

    // Validate using positive cover induction
    public Set<IntIntImmutablePair> validateWithPositiveCover(ExecutorService executor,
                                                              NegativeCover newNegativeCover,
                                                              int level, Set<BitSet> results) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Set<BitSet> candidates = this.root.getCandidatesAtDepth(level, results);

        List<Future<ValidationResult>> futures = new ArrayList<>(candidates.size());

        for (BitSet candidate : candidates) {
            futures.add(executor.submit(new ValidationTask(candidate)));
        }

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int validUCCCount   = 0;
        int invalidUCCCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            ValidationResult result = futures.get(i).get();

            // Valid RHS bits — confirm in posCover and store results
            if (result.status().equals(ValidationStatus.VALID)) {
                validUCCCount++;
                results.add((BitSet) result.ucc.clone());
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

        // All remaining checked without hitting threshold.
        return suggestions.isEmpty() ? null : suggestions;
    }

    public Set<IntIntImmutablePair> validateLockedCandidates(NegativeCover newNegativeCover,
                                                             Set<BitSet> candidateList,
                                                             Set<BitSet> confirmList) {

        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount = candidateList.size();
        int invalidUCCCount = 0;

        Iterator<BitSet> iterator = candidateList.iterator();

        while (iterator.hasNext()) {
            BitSet candidate = iterator.next();

            // Check posCover status first
            ValidationStatus status = this.root.containsUCCOrGeneralization(candidate);

            if (status.equals(ValidationStatus.INVALID)) {
                //Nothing is confirmed and Nothing is remaining to confirm
                iterator.remove();
                continue;
            }

            if (status.equals(ValidationStatus.VALID)) {
                confirmList.add((BitSet) candidate.clone());
                iterator.remove();
                continue;
            }

            // Validate remaining bits directly
            ValidationTask task = new ValidationTask(candidate);
            ValidationResult result = task.call();

            iterator.remove();

            // Valid RHS bits — confirm in posCover and store results
            if (result.status().equals(ValidationStatus.VALID)) {
                confirmList.add((BitSet) result.ucc.clone());
                this.root.markAsValidate(result.ucc());
            } else {
                invalidUCCCount++;
                specializePositiveCover(result.ucc());
                suggestions.addAll(result.suggestions());

                if (invalidUCCCount > totalCandidateCount * validationThreshold) {
                    // Cancel remaining queued tasks already
                    log.info("Cancelling Pending Tasks. TC: {}, IC: {}, VE: {}", totalCandidateCount, invalidUCCCount, totalCandidateCount * validationThreshold);

                    return suggestions;
                }
            }
        }

        return (candidateList.isEmpty() && suggestions.isEmpty()) ? null : suggestions;
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

        this.isInitialValidation = false;
    }

    // Specializes the positive cover for the non-UCC: agreeSet.
    private void specializePositiveCover(BitSet nonUCC) {
        List<BitSet> specUCCs = this.root.getUCCAndGeneralizations(nonUCC);

        for (BitSet specUCC : specUCCs) {
            this.root.removeUniqueColumnCombination(specUCC);

            for (int attr = this.numAttributes - 1; attr >= 0; attr--) {
                if (!nonUCC.get(attr)) {
                    specUCC.set(attr);
                    if (this.root.containsUCCOrGeneralization(specUCC) == ValidationStatus.INVALID) {
                        this.root.addUniqueColumnCombination(specUCC);
                    }
                    specUCC.clear(attr);
                }
            }
        }
    }

//    private ValidationStatus checkStatus(BitSet candidate) {
//        UCCTreeNode current = this.root;
//
//        for (int attr = candidate.nextSetBit(0); attr >= 0; attr = candidate.nextSetBit(attr + 1)) {
//            if(current.isValidatedUCC()){
//                return ValidationStatus.VALID;
//            }
//
//            if(current.isCandidateUCC()){
//                return ValidationStatus.POSSIBLE;
//            }
//
//            if (current.getChildren() == null || current.getChildren()[attr] == null) {
//                return ValidationStatus.INVALID;
//            }
//
//            current = current.getChildren()[attr];
//        }
//
//        // check the terminal node
//        if (current.isValidatedUCC()) return ValidationStatus.VALID;
//        if (current.isCandidateUCC()) return ValidationStatus.POSSIBLE;
//
//        return ValidationStatus.INVALID;
//    }
}