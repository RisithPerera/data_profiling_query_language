package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.profilers.requests.INDRequest;
import de.metathesis.profilers.requests.SearchSpace;
import de.metathesis.profilers.results.INDResult;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.Relation;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;

public class INDProfiler extends AbstractProfiler<INDRequest, INDResult> {
    private static final Logger log = LogManager.getLogger(INDProfiler.class);

    public INDProfiler(ExecutorService executor) {
        super(executor);
    }

    @Override
    public INDResult profile(INDRequest input) throws InputIterationException {

        if (input.lhs() instanceof SearchSpace.Free lhs && input.rhs() instanceof SearchSpace.Free rhs) {
            return profileFreeFree(lhs.relations(), rhs.relations(), lhs.level());
        }

        if (input.lhs() instanceof SearchSpace.Free lhs && input.rhs() instanceof SearchSpace.Lock rhs) {
            return profileFreeLock(lhs.relations(), rhs.attributes());
        }

        if (input.lhs() instanceof SearchSpace.Lock lhs && input.rhs() instanceof SearchSpace.Free rhs) {
            return profileLockFree(lhs.attributes(), rhs.relations());
        }

        if (input.lhs() instanceof SearchSpace.Lock lhs && input.rhs() instanceof SearchSpace.Lock rhs) {
            return profileLockLock(lhs.attributes(), rhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported INDRequest");
    }

    private INDResult profileFreeFree(int[] lhsRelations, int[] rhsRelations, int level) {
        log.debug("IND Profiling(Free: {}, Free: {}) At Level: {}", lhsRelations, rhsRelations, level);

        INDResult result = new INDResult();
        if(level == 0) return result;

        for (int lhsRel : lhsRelations) {
            Relation lhsRelation = profilingContext.getRelation(lhsRel);

            for (int rhsRel : rhsRelations) {
                // same relation size limit
                if (lhsRel == rhsRel && level > lhsRelation.getNumOfAttributes() / 2) continue;

                Relation rhsRelation = profilingContext.getRelation(rhsRel);

                // compute unary INDs for this pair if not already done
                this.profilingContext.getIndUnaryCover().ensureUnaryComputed(lhsRelation, rhsRelation);

                Map<Integer, BitSet> unaryINDs = this.profilingContext.getIndUnaryCover().getUnaryINDs(lhsRel, rhsRel);

                if(level == 1){
                    for(int lhsAttr : unaryINDs.keySet()) {
                        BitSet rhsCols = unaryINDs.get(lhsAttr);
                        for (int rhsAttr = rhsCols.nextSetBit(0); rhsAttr >= 0; rhsAttr = rhsCols.nextSetBit(rhsAttr + 1)) {
                            result.add(new AttributeBitSet(lhsRel, lhsAttr), new AttributeBitSet(rhsRel, rhsAttr));
                        }
                    }
                    continue;
                }

                int[] lhsPool = unaryINDs.keySet().stream().mapToInt(Integer::intValue).toArray();
                if (lhsPool.length < level) continue;

                int[][] lhsCombinations = Utility.combinations(lhsPool, level);

                outer: for(int[] lhsCols : lhsCombinations){
                    // for each lhs position, get valid rhs cols from unary INDs
                    BitSet[] validRhsPerPosition = new BitSet[lhsCols.length];
                    for (int pos = 0; pos < lhsCols.length; pos++) {
                        BitSet rhsCandidates = (BitSet) unaryINDs.get(lhsCols[pos]).clone();

                        // same relation — remove lhs cols which included in given rhs
                        if (lhsRel == rhsRel) {
                            for (int lhsAttr : lhsCols) rhsCandidates.clear(lhsAttr);
                        }

                        if (rhsCandidates.isEmpty()) continue outer;
                        validRhsPerPosition[pos] = rhsCandidates;
                    }

                    String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);

                    for (int[] rhsCols : Utility.cartesianProduct(validRhsPerPosition)) {
                        String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);
                        if (isIncluded(lhsTuples, rhsTuples)) {
                            result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, rhsCols));
                        }
                    }
                }
            }
        }

        return result;
    }

    //Checked
    private INDResult profileFreeLock(int[] lhsRelations, ObjectOpenHashSet<AttributeBitSet> rhsAttrs) {
        log.debug("IND Profiling(Free: {}, Lock: {} Candidates)", lhsRelations, rhsAttrs.size());

        INDResult result = new INDResult();

        for (AttributeBitSet rhs : rhsAttrs) {
            if(rhs.isEmpty()) continue;

            int rhsRel = rhs.getRelationIndex();
            int[] rhsCols = rhs.getAttributeIndexArray();
            Relation rhsRelation = profilingContext.getRelation(rhsRel);

            outer: for (int lhsRel : lhsRelations) {
                // same relation size limit
                Relation lhsRelation = profilingContext.getRelation(lhsRel);
                if (lhsRel == rhsRel && rhsCols.length > lhsRelation.getNumOfAttributes() / 2) continue;

                // ensure unary INDs computed for this pair
                this.profilingContext.getIndUnaryCover().ensureUnaryComputed(lhsRelation, rhsRelation);

                if (rhsCols.length == 1) {
                    BitSet lhsCols = this.profilingContext.getIndUnaryCover().getLhsCols(lhsRel, rhsRel, rhsCols[0]);
                    for (int attr = lhsCols.nextSetBit(0); attr >= 0; attr = lhsCols.nextSetBit(attr + 1)) {
                        result.add(new AttributeBitSet(lhsRel, attr), new AttributeBitSet(rhsRel, rhsCols));
                    }
                    continue;
                }

                // for each rhs position, get valid lhs cols from unary INDs
                BitSet[] validLhsPerPosition = new BitSet[rhsCols.length];
                for (int pos = 0; pos < rhsCols.length; pos++) {
                    BitSet lhsCandidates = this.profilingContext.getIndUnaryCover().getLhsCols(lhsRel, rhsRel, rhsCols[pos]);

                    // same relation — remove lhs cols which included in given rhs
                    if (lhsRel == rhsRel) {
                        for (int rhsAttr : rhsCols) lhsCandidates.clear(rhsAttr);
                    }

                    if (lhsCandidates.isEmpty()) continue outer;
                    validLhsPerPosition[pos] = lhsCandidates;
                }

                String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);

                for (int[] lhsCols : Utility.cartesianProduct(validLhsPerPosition)) {
                    String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);
                    if (isIncluded(lhsTuples, rhsTuples)) {
                        result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, rhsCols));
                    }
                }
            }
        }

        return result;
    }

    //Checked
    private INDResult profileLockFree(ObjectOpenHashSet<AttributeBitSet> lhsAttrs, int[] rhsRelations){
        log.debug("IND Profiling(Lock: {} Candidates, Free: {})", lhsAttrs.size(), rhsRelations);
        INDResult result = new INDResult();

        for (AttributeBitSet lhs : lhsAttrs) {
            int lhsRel = lhs.getRelationIndex();
            int[] lhsCols = lhs.getAttributeIndexArray();
            Relation lhsRelation = profilingContext.getRelation(lhsRel);

            outer: for (int rhsRel : rhsRelations) {
                // same relation size limit
                Relation rhsRelation = profilingContext.getRelation(rhsRel);
                if (lhsRel == rhsRel && lhsCols.length > lhsRelation.getNumOfAttributes() / 2) continue;

                // ensure unary INDs computed for this pair
                this.profilingContext.getIndUnaryCover().ensureUnaryComputed(lhsRelation, rhsRelation);

                if(lhsCols.length == 1){
                    BitSet rhsCols = this.profilingContext.getIndUnaryCover().getRhsCols(lhsRel, rhsRel, lhsCols[0]);
                    for (int rhsAttr = rhsCols.nextSetBit(0); rhsAttr >= 0; rhsAttr = rhsCols.nextSetBit(rhsAttr + 1)) {
                        result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, rhsAttr));
                    }
                    continue;
                }

                // for each lhs position, get valid rhs cols from unary INDs
                BitSet[] validRhsPerPosition = new BitSet[lhsCols.length];
                for (int pos = 0; pos < lhsCols.length; pos++) {
                    BitSet rhsCandidates = this.profilingContext.getIndUnaryCover().getRhsCols(lhsRel, rhsRel, lhsCols[pos]);

                    // same relation — remove lhs cols which included in given rhs
                    if (lhsRel == rhsRel) {
                        for (int lhsAttr : lhsCols) rhsCandidates.clear(lhsAttr);
                    }

                    if (rhsCandidates.isEmpty()) continue outer;
                    validRhsPerPosition[pos] = rhsCandidates;
                }

                String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);

                for (int[] rhsCols : Utility.cartesianProduct(validRhsPerPosition)) {
                    String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);
                    if (isIncluded(lhsTuples, rhsTuples)) {
                        result.add(new AttributeBitSet(lhsRel, lhsCols), new AttributeBitSet(rhsRel, rhsCols));
                    }
                }
            }
        }

        return result;
    }

    //Checked
    private INDResult profileLockLock(ObjectOpenHashSet<AttributeBitSet> lhsAttrs, ObjectOpenHashSet<AttributeBitSet> rhsAttrs) {
        log.debug("IND Profiling(Lock: {} Candidates, Lock: {} Candidates)", lhsAttrs.size(), rhsAttrs.size());
        INDResult result = new INDResult();

        for (AttributeBitSet lhs : lhsAttrs) {
            int lhsRel = lhs.getRelationIndex();
            int[] lhsCols = lhs.getAttributeIndexArray();
            Relation lhsRelation = profilingContext.getRelation(lhsRel);
            String[] lhsTuples = buildTuples(lhsRelation.getAttributeValues(), lhsCols);

            outer: for (AttributeBitSet rhs : rhsAttrs) {
                int rhsRel = rhs.getRelationIndex();
                int[] rhsCols = rhs.getAttributeIndexArray();

                // sizes must match
                if (rhsCols.length != lhsCols.length) continue;

                // same relation size limit
                if (lhsRel == rhsRel && lhsCols.length > lhsRelation.getNumOfAttributes() / 2) continue;

                Relation rhsRelation = profilingContext.getRelation(rhsRel);

                //LHS is locked so no permutation, check position by position
                this.profilingContext.getIndUnaryCover().ensureUnaryComputed(lhsRelation, rhsRelation);

                for (int pos = 0; pos < lhsCols.length; pos++) {
                    if (!this.profilingContext.getIndUnaryCover().getRhsCols(lhsRel, rhsRel, lhsCols[pos]).get(rhsCols[pos])) {
                        continue outer;
                    }
                }

                // full tuple check
                String[] rhsTuples = buildTuples(rhsRelation.getAttributeValues(), rhsCols);

                if (isIncluded(lhsTuples, rhsTuples)) {
                    result.add(lhs, rhs);
                }
            }
        }
        return result;
    }

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

