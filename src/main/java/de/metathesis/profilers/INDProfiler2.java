package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.INDUnaryCover;
import de.metathesis.structures.Relation;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.INDResult;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;

public class INDProfiler2 extends AbstractProfiler<INDRequest, INDResult> {
    private static final Logger log = LogManager.getLogger(INDProfiler2.class);

    private final INDUnaryCover unaryCover = new INDUnaryCover();

    public INDProfiler2(ExecutorService executor) {
        super(executor);
    }

    @Override
    public INDResult profile(INDRequest input) throws InputIterationException {

        if (input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileFreeFree(lhs.relations(), rhs.relations(), lhs.level());
        }

        if (input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs) {
            return profileFreeLock(lhs.relations(), rhs.attributes());
        }

        if (input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileLockFree(lhs.attributes(), rhs.relations());
        }

        if (input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs) {
            return profileLockLock(lhs.attributes(), rhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported INDRequest");
    }

    private INDResult profileFreeFree(int[] lhsRelations, int[] rhsRelations, int level) {

        INDResult result = new INDResult();

        for (int lhsRel : lhsRelations) {
            Relation lhsRelation = preprocessor.getRelation(lhsRel);

            for (int rhsRel : rhsRelations) {
                // same relation size limit
                if (lhsRel == rhsRel && level > lhsRelation.getNumOfAttributes() / 2) continue;

                Relation rhsRelation = preprocessor.getRelation(rhsRel);

                // compute unary INDs for this pair if not already done
                unaryCover.ensureUnaryComputed(lhsRelation, rhsRelation);

                // flatten unary bindings for this pair into (lhsCol, rhsCol) pairs
                List<int[]> unaryBindings = unaryCover.getBindings(lhsRel, rhsRel);

                if (level == 1) {
                    for (int[] binding : unaryBindings) {
                        result.add(
                                new AttributeBitSet(lhsRel, new int[]{binding[0]}),
                                new AttributeBitSet(rhsRel, new int[]{binding[1]})
                        );
                    }
                    continue;
                }

                // level N — need at least level bindings to form a candidate
                if (unaryBindings.size() < level) continue;

                combineBindings(unaryBindings, level, 0, new int[level], new int[level], 0, lhsRel, rhsRel, lhsRelation, rhsRelation, result);
            }
        }

        return result;
    }

    private INDResult profileFreeLock(int[] lhsRelations, ObjectOpenHashSet<AttributeBitSet> rhsAttrs) {
        INDResult result = new INDResult();

        for (AttributeBitSet rhs : rhsAttrs) {
            int rhsRel = rhs.getRelationIndex();
            int[] rhsCols = rhs.getAttributeIndexArray();
            Relation rhsRelation = preprocessor.getRelation(rhsRel);

            outer: for (int lhsRel : lhsRelations) {
                // same relation size limit
                Relation lhsRelation = preprocessor.getRelation(lhsRel);
                if (lhsRel == rhsRel && rhsCols.length > lhsRelation.getNumOfAttributes() / 2) continue;

                // ensure unary INDs computed for this pair
                unaryCover.ensureUnaryComputed(lhsRelation, rhsRelation);

                if (rhsCols.length == 1) {
                    BitSet lhsCols = unaryCover.getLhsCols(lhsRel, rhsRel, rhsCols[0]);
                    for (int attr = lhsCols.nextSetBit(0); attr >= 0; attr = lhsCols.nextSetBit(attr + 1)) {
                        result.add(new AttributeBitSet(lhsRel, attr), new AttributeBitSet(rhsRel, rhsCols));
                    }
                    continue;
                }

                // for each rhs position, get valid lhs cols from unary INDs
                BitSet[] validLhsPerPosition = new BitSet[rhsCols.length];
                for (int pos = 0; pos < rhsCols.length; pos++) {
                    validLhsPerPosition[pos] = unaryCover.getLhsCols(lhsRel, rhsRel, rhsCols[pos]);
                    if (validLhsPerPosition[pos].isEmpty()) {
                        continue outer; // fast exit
                    }
                }

                String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);

                for (int[] lhsCols : Utility.cartesianProduct(validLhsPerPosition)) {
                    if (lhsRel == rhsRel && !Utility.isDisjoint(lhsCols, rhsCols)) continue;

                    String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);
                    if (isIncluded(lhsTuples, rhsTuples)) {
                        result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, rhsCols));
                    }
                }
            }
        }

        return result;
    }

    private INDResult profileLockFree(ObjectOpenHashSet<AttributeBitSet> lhsAttrs, int[] rhsRelations){
        INDResult result = new INDResult();

        for (AttributeBitSet lhs : lhsAttrs) {
            int lhsRel = lhs.getRelationIndex();
            int[] lhsCols = lhs.getAttributeIndexArray();
            Relation lhsRelation = preprocessor.getRelation(lhsRel);

            outer: for (int rhsRel : rhsRelations) {
                // same relation size limit
                Relation rhsRelation = preprocessor.getRelation(rhsRel);
                if (lhsRel == rhsRel && lhsCols.length > lhsRelation.getNumOfAttributes() / 2) continue;

                // ensure unary INDs computed for this pair
                unaryCover.ensureUnaryComputed(lhsRelation, rhsRelation);

                if(lhsCols.length == 1){
                    BitSet rhsCols = this.unaryCover.getRhsCols(lhsRel, lhsCols[0], rhsRel);
                    for (int attr = rhsCols.nextSetBit(0); attr >= 0; attr = rhsCols.nextSetBit(attr + 1)) {
                        result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, attr));
                    }
                    continue;
                }

                // for each lhs position, get valid rhs cols from unary INDs
                // unaryINDs keyed by rhsCol -> lhsCols, so need reverse lookup here
                BitSet[] validRhsPerPosition = new BitSet[lhsCols.length];
                for (int pos = 0; pos < lhsCols.length; pos++) {
                    validRhsPerPosition[pos] = this.unaryCover.getRhsCols(lhsRel, lhsCols[pos], rhsRel);
                    if (validRhsPerPosition[pos].isEmpty()){
                        continue outer; // fast exit
                    }
                }

                String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);

                for (int[] rhsCols : Utility.cartesianProduct(validRhsPerPosition)) {
                    if (lhsRel == rhsRel && !Utility.isDisjoint(lhsCols, rhsCols)){
                        continue;
                    }

                    String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);
                    if (isIncluded(lhsTuples, rhsTuples)) {
                        result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, rhsCols));
                    }
                }
            }
        }

        return result;
    }

    private INDResult profileLockLock(ObjectOpenHashSet<AttributeBitSet> lhsAttrs, ObjectOpenHashSet<AttributeBitSet> rhsAttrs) {

        INDResult result = new INDResult();

        for (AttributeBitSet lhs : lhsAttrs) {
            int lhsRel = lhs.getRelationIndex();
            int[] lhsCols = lhs.getAttributeIndexArray();
            int arity = lhsCols.length;

            outer: for (AttributeBitSet rhs : rhsAttrs) {
                int rhsRel = rhs.getRelationIndex();
                int[] rhsCols = rhs.getAttributeIndexArray();

                // sizes must match
                if (rhsCols.length != arity) continue;

                // same relation size limit
                Relation lhsRelation = preprocessor.getRelation(lhsRel);
                if (lhsRel == rhsRel && arity > lhsRelation.getNumOfAttributes() / 2) continue;

                Relation rhsRelation = preprocessor.getRelation(rhsRel);

                // unary gate — LHS is locked so no permutation, check position by position
                unaryCover.ensureUnaryComputed(lhsRelation, rhsRelation);

                for (int pos = 0; pos < arity; pos++) {
                    if (!unaryCover.contains(lhsRel, lhsCols[pos], rhsRel, rhsCols[pos])) {
                        continue outer;
                    }
                }

                // full tuple check

                String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);
                String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);

                if (isIncluded(lhsTuples, rhsTuples)) {
                    result.add(lhs, rhs);
                }
            }
        }
        return result;
    }

    private void findLHSForRHS(int[] rhsCols, int rhsRel, int lhsRel, INDResult result){

        int arity = rhsCols.length;

        // for each rhs position, get valid lhs cols from unary INDs
        BitSet[] validLhsPerPosition = new BitSet[arity];
        for (int pos = 0; pos < arity; pos++) {
            validLhsPerPosition[pos] = this.unaryCover.getLhsCols(lhsRel, rhsRel, rhsCols[pos]);
            if (validLhsPerPosition[pos].isEmpty()) return; // fast exit
        }

        Relation lhsRelation = preprocessor.getRelation(lhsRel);
        Relation rhsRelation = preprocessor.getRelation(rhsRel);

        long time = System.currentTimeMillis();
        String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);
        log.info("Time for buildTuples R:{}, rhsCols: {} time: {}ms", rhsRelation.getIndex(), rhsCols, (System.currentTimeMillis() - time)); //TODO: Here Arity Size1: Needs to Check

        combineLHS(validLhsPerPosition, new int[arity], 0, rhsCols, rhsRel, lhsRel, lhsRelation, rhsTuples, result);
    }

    private void combineLHS(BitSet[] validPerPos, int[] current, int pos, int[] rhsCols, int rhsRel, int lhsRel,
                            Relation lhsRelation, String[] rhsTuples, INDResult result) {

        if (pos == current.length) {
            String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), current);
            if (isIncluded(lhsTuples, rhsTuples)) {
                result.add(
                        new AttributeBitSet(lhsRel, current.clone()),
                        new AttributeBitSet(rhsRel, rhsCols)
                );
            }
            return;
        }

        // iterate BitSet instead of IntOpenHashSet
        for (int lhsCol = validPerPos[pos].nextSetBit(0);
             lhsCol >= 0;
             lhsCol = validPerPos[pos].nextSetBit(lhsCol + 1)) {

            boolean duplicate = false;
            for (int i = 0; i < pos; i++) {
                if (current[i] == lhsCol) { duplicate = true; break; }
            }
            if (duplicate) continue;

            if (lhsRel == rhsRel) {
                boolean inRhs = false;
                for (int rc : rhsCols) {
                    if (rc == lhsCol) { inRhs = true; break; }
                }
                if (inRhs) continue;
            }

            current[pos] = lhsCol;
            combineLHS(validPerPos, current, pos + 1, rhsCols, rhsRel, lhsRel, lhsRelation, rhsTuples, result);
        }
    }

    private void findRHSForLHS(int[] lhsCols, int lhsRel, int rhsRel, INDResult result){

        int arity = lhsCols.length;

        // for each lhs position, get valid rhs cols from unary INDs
        // unaryINDs keyed by rhsCol -> lhsCols, so need reverse lookup here
        BitSet[] validRhsPerPosition = new BitSet[arity];
        for (int pos = 0; pos < arity; pos++) {
            validRhsPerPosition[pos] = this.unaryCover.getRhsCols(lhsRel, lhsCols[pos], rhsRel);
            if (validRhsPerPosition[pos].isEmpty()) return; // fast exit
        }

        Relation lhsRelation = preprocessor.getRelation(lhsRel);
        Relation rhsRelation = preprocessor.getRelation(rhsRel);
        String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);

        combineRHS(validRhsPerPosition, new int[arity], 0, lhsCols, lhsRel, rhsRel, rhsRelation, lhsTuples, result);
    }

    private void combineRHS(BitSet[] validPerPos, int[] current, int pos,
                            int[] lhsCols, int lhsRel, int rhsRel,
                            Relation rhsRelation, String[] lhsTuples, INDResult result) {

        if (pos == current.length) {
            String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), current);
            if (isIncluded(lhsTuples, rhsTuples)) {
                result.add(
                        new AttributeBitSet(lhsRel, lhsCols),
                        new AttributeBitSet(rhsRel, current.clone())
                );
            }
            return;
        }

        for (int rhsCol = validPerPos[pos].nextSetBit(0);
             rhsCol >= 0;
             rhsCol = validPerPos[pos].nextSetBit(rhsCol + 1)) {

            boolean duplicate = false;
            for (int i = 0; i < pos; i++) {
                if (current[i] == rhsCol) { duplicate = true; break; }
            }

            if (duplicate) continue;

            if (lhsRel == rhsRel) {
                boolean inLhs = false;
                for (int lc : lhsCols) {
                    if (lc == rhsCol) { inLhs = true; break; }
                }
                if (inLhs) continue;
            }

            current[pos] = rhsCol;
            combineRHS(validPerPos, current, pos + 1, lhsCols, lhsRel, rhsRel, rhsRelation, lhsTuples, result);
        }
    }

    private void combineBindings(List<int[]> bindings, int level, int start,
                                 int[] lhsCols, int[] rhsCols, int pos,
                                 int lhsRel, int rhsRel,
                                 Relation lhsRelation, Relation rhsRelation,
                                 INDResult result) {

        if (pos == level) {
            // full tuple check
            String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);
            String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);
            if (isIncluded(lhsTuples, rhsTuples)) {
                result.add(
                        new AttributeBitSet(lhsRel, lhsCols.clone()),
                        new AttributeBitSet(rhsRel, rhsCols.clone())
                );
            }
            return;
        }

        for (int i = start; i <= bindings.size() - (level - pos); i++) {
            int[] binding = bindings.get(i);
            int lhsCol = binding[0];
            int rhsCol = binding[1];

            // lhs cols must be distinct
            boolean lhsDup = false;
            for (int j = 0; j < pos; j++) {
                if (lhsCols[j] == lhsCol) { lhsDup = true; break; }
            }
            if (lhsDup) continue;

            // rhs cols must be distinct
            boolean rhsDup = false;
            for (int j = 0; j < pos; j++) {
                if (rhsCols[j] == rhsCol) { rhsDup = true; break; }
            }
            if (rhsDup) continue;

            // same relation — lhs and rhs cols must be disjoint
            if (lhsRel == rhsRel) {
                boolean conflict = false;
                for (int j = 0; j < pos; j++) {
                    if (lhsCols[j] == rhsCol || rhsCols[j] == lhsCol) {
                        conflict = true; break;
                    }
                }
                if (!conflict) {
                    // also check current binding against itself
                    if (lhsCol == rhsCol) conflict = true;
                }
                if (conflict) continue;
            }

            lhsCols[pos] = lhsCol;
            rhsCols[pos] = rhsCol;

            combineBindings(bindings, level, i + 1, lhsCols, rhsCols, pos + 1, lhsRel, rhsRel, lhsRelation, rhsRelation, result);
        }
    }

    // Calculate all unary INDs between given two tables


    /* ------------------- Utility Methods ------------------- */

    // build tuples for an ordered int[] of cols (not BitSet — preserves permutation order)
    private String[] buildTuples(String[][] columns, int[] orderedCols) {
        int rows = columns[0].length;
        String[] tmp = new String[rows];
        StringBuilder tupleBuilder = new StringBuilder();

        for (int r = 0; r < rows; r++) {
            tupleBuilder.setLength(0);
            for (int i = 0; i < orderedCols.length; i++) {
                if (i > 0) tupleBuilder.append('\u0001');
                tupleBuilder.append(columns[orderedCols[i]][r]);
            }
            tmp[r] = tupleBuilder.toString();
        }

        Arrays.sort(tmp);

        int unique = 0;
        for (int i = 0; i < tmp.length; i++) {
            if (i == 0 || !tmp[i].equals(tmp[i - 1])) {
                tmp[unique++] = tmp[i];
            }
        }
        return Arrays.copyOf(tmp, unique);
    }

    private boolean isIncluded(String[] lhs, String[] rhs) {
        int i = 0, j = 0;
        while (i < lhs.length && j < rhs.length) {
            if(lhs[i].isEmpty()){
                i++;
                continue;
            }

            int cmp = lhs[i].compareTo(rhs[j]);
            if (cmp == 0) {
                i++; j++;
            } else if (cmp > 0) {
                j++;
            } else {
                return false;
            }
        }
        return i == lhs.length;
    }
}

