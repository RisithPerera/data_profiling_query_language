package de.metathesis.profilers.validators;

import de.metathesis.profilers.structures.NegativeCover;
import de.metathesis.profilers.structures.UCCTreeNode;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
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
        this.root.init();
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
                boolean status = UCCValidator.this.plis[uccAttr].isUnique();
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
            boolean isValid,
            Set<IntIntImmutablePair> suggestions) {
    }

    public Set<IntIntImmutablePair> validateFree(ExecutorService executor, NegativeCover newNegativeCover,
                                                 int level, Set<BitSet> results) throws ExecutionException, InterruptedException {
        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount = 0;
        int invalidUCCCount = 0;

        // Validate until no more possible candidates at size <= level
        while (true) {
            Set<BitSet> candidates = this.root.getLhsPathsAtDepth(level);
            if (candidates.isEmpty()) break;

            List<Future<ValidationResult>> futures = new ArrayList<>(candidates.size());
            for (BitSet candidate : candidates) {
                futures.add(executor.submit(new ValidationTask(candidate)));
            }

            for (Future<ValidationResult> future : futures) {
                ValidationResult result = future.get();
                totalCandidateCount++;

                if (result.isValid()) {
                    this.root.markAsValidate(result.ucc());
                } else {
                    invalidUCCCount++;
                    this.root.specializePositiveCover(result.ucc());
                    suggestions.addAll(result.suggestions());
                }
            }

            if (invalidUCCCount > totalCandidateCount * validationThreshold) {
                log.debug("Back to sampling. TC: {}, IC: {}, VE: {}", totalCandidateCount, invalidUCCCount, totalCandidateCount * validationThreshold);
                return suggestions;
            }
        }

        // Collect all validated UCCs on the given level size
        this.root.getValidatedUCCsAtDepth(level, results);

        return null;
    }

    public Set<IntIntImmutablePair> validateLock(NegativeCover newNegativeCover, Set<BitSet> candidateList, Set<BitSet> confirmList) {

        inductPositiveCover(newNegativeCover);

        Set<IntIntImmutablePair> suggestions = new HashSet<>();
        int totalCandidateCount = candidateList.size();
        int invalidUCCCount = 0;

        Iterator<BitSet> iterator = candidateList.iterator();

        while (iterator.hasNext()) {
            BitSet candidate = iterator.next();

            // Check posCover status first
            int status = this.root.getStatusForLhsPath(candidate);

            if (status == -1) {
                //Nothing is confirmed and Nothing is remaining to confirm
                iterator.remove();
                continue;
            }

            if (status == 1) {
                confirmList.add((BitSet) candidate.clone());
                iterator.remove();
                continue;
            }

            // Validate remaining bits directly
            ValidationTask task = new ValidationTask(candidate);
            ValidationResult result = task.call();

            iterator.remove();

            // Valid RHS bits confirm in posCover and store results
            if (result.isValid()) {
                confirmList.add((BitSet) result.ucc.clone());
                this.root.markAsValidate(result.ucc());
            } else {
                invalidUCCCount++;
                this.root.specializePositiveCover(result.ucc());
                suggestions.addAll(result.suggestions());

                if (invalidUCCCount > totalCandidateCount * validationThreshold) {
                    // Cancel remaining queued tasks already
                    log.debug("Back to sampling. TC: {}, IC: {}, VE: {}", totalCandidateCount, invalidUCCCount, totalCandidateCount * validationThreshold);

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
                this.root.specializePositiveCover(nonUCC);
            }
        }

        this.isInitialValidation = false;
    }
}