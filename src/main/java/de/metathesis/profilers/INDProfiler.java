package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Utility;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.INDResult;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Executor;

public class INDProfiler extends AbstractProfiler<INDRequest, INDResult> {

    /* ===================== CACHES ===================== */

    // AttributeBitSet -> sorted unique tuple list
    private final Object2ObjectMap<AttributeBitSet, String[]> cachedTuples = new Object2ObjectOpenHashMap<>();

    // Transitive closure: lhs -> {rhs1, rhs2, ...}
    private final Object2ObjectMap<AttributeBitSet, ObjectOpenHashSet<AttributeBitSet>> indClosure = new Object2ObjectOpenHashMap<>();

    private final LongSet fullyCheckedCC = new LongOpenHashSet();

    public INDProfiler(Executor executor) {
        super(executor);
    }

    /* ===================== ENTRY POINT ===================== */

    @Override
    public INDResult profile(INDRequest input) throws InputIterationException {

        if (input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileCC(lhs.relations(), rhs.relations(), lhs.level());
        }

        if (input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs) {
            return profileCC_Locked(lhs.relations(), rhs.attributes(), lhs.level());
        }

        if (input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileLocked_CC(lhs.attributes(), rhs.relations(), rhs.level());
        }

        if (input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs) {
            return profileLocked(lhs.attributes(), rhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported INDRequest");
    }

    /* ===================== VARIANTS ===================== */

    private INDResult profileCC(int[] lhsRelations, int[] rhsRelations, int level) throws InputIterationException {

        List<AttributeBitSet> lhsAttrs = new ArrayList<>();
        List<AttributeBitSet> rhsAttrs = new ArrayList<>();

        for (int r : lhsRelations) {
            lhsAttrs.addAll(List.of(preprocessor.generateApriori(r, level)));
        }
        for (int r : rhsRelations) {
            rhsAttrs.addAll(List.of(preprocessor.generateApriori(r, level)));
        }

        INDResult result = profileGeneric(lhsAttrs, rhsAttrs);

        for (int l : lhsRelations) {
            for (int r : rhsRelations) {
                fullyCheckedCC.add(Utility.compositeKey(l, r, level));
            }
        }

        return result;
    }

    private INDResult profileCC_Locked(int[] lhsRelations, ObjectOpenHashSet<AttributeBitSet> rhsAttrs, int level) throws InputIterationException {

        List<AttributeBitSet> lhsAttrs = new ArrayList<>();
        for (int r : lhsRelations) {
            lhsAttrs.addAll(List.of(preprocessor.generateApriori(r, level)));
        }

        return profileGeneric(lhsAttrs, rhsAttrs);
    }

    private INDResult profileLocked_CC(ObjectOpenHashSet<AttributeBitSet> lhsAttrs, int[] rhsRelations, int level) throws InputIterationException {

        List<AttributeBitSet> rhsAttrs = new ArrayList<>();
        for (int r : rhsRelations) {
            rhsAttrs.addAll(List.of(preprocessor.generateApriori(r, level)));
        }

        return profileGeneric(lhsAttrs, rhsAttrs);
    }

    private INDResult profileLocked(ObjectOpenHashSet<AttributeBitSet> lhsAttrs, ObjectOpenHashSet<AttributeBitSet> rhsAttrs) throws InputIterationException {

        return profileGeneric(lhsAttrs, rhsAttrs);
    }

    private INDResult profileGeneric(Iterable<AttributeBitSet> lhsAttrs, Iterable<AttributeBitSet> rhsAttrs) throws InputIterationException {

        INDResult result = new INDResult();

        for (AttributeBitSet lhs : lhsAttrs) {

            String[] lhsTuples = getTuples(lhs);

            for (AttributeBitSet rhs : rhsAttrs) {
                if (isOverlap(lhs, rhs)){
                    continue;
                }

                if (upwardPrune(lhs, rhs)){
                    continue;
                }

                if (isImplied(lhs, rhs)){
                    result.add(lhs, rhs);
                    continue;
                }

                String[] rhsTuples = getTuples(rhs);

                if (lhsTuples.length > rhsTuples.length){
                    continue;
                }

                if (isIncluded(lhsTuples, rhsTuples)) {
                    result.add(lhs, rhs);
                    registerIND(lhs, rhs);
                }
            }
        }
        return result;
    }

    /* ===================== PRUNING ===================== */

    /**
     * Note that X and Y are distinct lists (X ∩ Y=∅),
     * because INDs with overlaps have basically no practical use cases.
     */
    private boolean isOverlap(AttributeBitSet a, AttributeBitSet b) {
        if(a.equals(b)) return true;

        return a.getRelationIndex() == b.getRelationIndex() && !a.intersect(b).isEmpty();
    }

    /**
     * Upwards Pruning: Generate {A,B} ⊆ {C,D} only if {A} ⊆ {C} and {B} ⊆ {D} are both true!
     */
    private boolean upwardPrune(AttributeBitSet lhs, AttributeBitSet rhs) {
        int level = lhs.size();
        if (level <= 1) return false;

        int lhsRel = lhs.getRelationIndex();
        int rhsRel = rhs.getRelationIndex();

        // Only prune if previous level was fully checked
        if (!fullyCheckedCC.contains(Utility.compositeKey(lhsRel, rhsRel, level - 1))) {
            return false;
        }

        // Apriori-style pruning
        List<AttributeBitSet> lhsSubSets = lhs.immediateSubsets();
        List<AttributeBitSet> rhsSubSets = rhs.immediateSubsets();

        for (int i = 0; i <= lhsSubSets.size() - 1; i++) {
            AttributeBitSet lhsSubset = lhsSubSets.get(i);
            ObjectOpenHashSet<AttributeBitSet> knownINDs = indClosure.get(lhsSubset);
            if (knownINDs == null || knownINDs.isEmpty()){
                return true;
            }

            AttributeBitSet rhsSub = rhsSubSets.get(i);
            if (!knownINDs.contains(rhsSub)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Same level: If {A} ⊆ {B} and {B} ⊆ {C}, then {A} ⊆ {C} must also be true.
     */
    private boolean isImplied(AttributeBitSet lhs, AttributeBitSet rhs) {
        return isImpliedDfs(lhs, rhs, new ObjectOpenHashSet<>());
    }

    private boolean isImpliedDfs(AttributeBitSet current, AttributeBitSet target, ObjectOpenHashSet<AttributeBitSet> visited) {

        if (!visited.add(current)) return false;

        ObjectOpenHashSet<AttributeBitSet> next = indClosure.get(current);
        if (next == null) return false;

        if (next.contains(target)) return true;

        for (AttributeBitSet n : next) {
            if (isImpliedDfs(n, target, visited)) {
                return true;
            }
        }
        return false;
    }

    private void registerIND(AttributeBitSet lhs, AttributeBitSet rhs) {
        indClosure.computeIfAbsent(lhs, k -> new ObjectOpenHashSet<>()).add(rhs);

        if (indClosure.containsKey(rhs)) {
            indClosure.get(lhs).addAll(indClosure.get(rhs));
        }
    }

    /* ===================== TUPLES ===================== */

    private String[] getTuples(AttributeBitSet abs) throws InputIterationException {
        String[] cached = this.cachedTuples.get(abs);
        if (cached != null) {
            return cached;
        }

        String[][] records = this.preprocessor.getColumnWiseDataOf(abs.getRelationIndex());
        String[] tuples = buildTuples(records, abs);

        this.cachedTuples.put(abs, tuples);
        return tuples;
    }


    private String[] buildTuples(String[][] columns, AttributeBitSet attrs) {
        int rows = columns[0].length;
        int[] cols = attrs.getAttributeIndexSet().stream().toArray();

        String[] tmp = new String[rows];

        for (int r = 0; r < rows; r++) {
            StringJoiner joiner = new StringJoiner("\u0001");
            for (int c : cols) {
                joiner.add(columns[c][r]);
            }
            tmp[r] = joiner.toString();
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

    /* ===================== INCLUSION ===================== */

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

