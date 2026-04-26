package de.metathesis.structures;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class INDUnaryCover {
    private static final Logger log = LogManager.getLogger(INDUnaryCover.class);

    // forward:  unaryINDs.get(lhsRel).get(rhsRel).get(rhsCol) = set of lhsCols
    private final Map<Integer, Map<Integer, Map<Integer, BitSet>>> forward = new ConcurrentHashMap<>();

    // reverse: unaryINDsRev.get(lhsRel).get(rhsRel).get(lhsCol) = set of rhsCols
    private final Map<Integer, Map<Integer, Map<Integer, BitSet>>> reverse = new ConcurrentHashMap<>();

    // tracks which (lhsRel, rhsRel) pairs have been computed for unary INDs
    private final ConcurrentHashMap<Long, Boolean> computedPairs = new ConcurrentHashMap<>();

    private void add(int lhsRel, int lhsCol, int rhsRel, int rhsCol) {
        // forward
        forward.computeIfAbsent(lhsRel, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(rhsRel, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(rhsCol, k -> new BitSet())
                .set(lhsCol);

        // reverse
        reverse.computeIfAbsent(lhsRel, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(rhsRel, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(lhsCol, k -> new BitSet())
                .set(rhsCol);
    }

    public BitSet getLhsCols(int lhsRel, int rhsRel, int rhsCol) {
        return forward
                .getOrDefault(lhsRel, Collections.emptyMap())
                .getOrDefault(rhsRel, Collections.emptyMap())
                .getOrDefault(rhsCol, new BitSet());
    }

    public BitSet getRhsCols(int lhsRel, int lhsCol, int rhsRel) {
        return reverse
                .getOrDefault(lhsRel, Collections.emptyMap())
                .getOrDefault(rhsRel, Collections.emptyMap())
                .getOrDefault(lhsCol, new BitSet());
    }

    public boolean contains(int lhsRel, int lhsCol, int rhsRel, int rhsCol) {
        return reverse
                .getOrDefault(lhsRel, Collections.emptyMap())
                .getOrDefault(rhsRel, Collections.emptyMap())
                .getOrDefault(lhsCol, new BitSet())
                .get(rhsCol);
    }

    public List<int[]> getBindings(int lhsRel, int rhsRel) {
        Map<Integer, BitSet> rhsMap = forward
                .getOrDefault(lhsRel, Collections.emptyMap())
                .getOrDefault(rhsRel, Collections.emptyMap());

        List<int[]> bindings = new ArrayList<>();
        for (Map.Entry<Integer, BitSet> entry : rhsMap.entrySet()) {
            int rhsCol = entry.getKey();
            BitSet lhsCols = entry.getValue();
            for (int lhsCol = lhsCols.nextSetBit(0); lhsCol >= 0; lhsCol = lhsCols.nextSetBit(lhsCol + 1)) {
                bindings.add(new int[]{lhsCol, rhsCol});
            }
        }
        return bindings;
    }

    public void ensureUnaryComputed(Relation lhsRelation, Relation rhsRelation) {
        int lhsRel = lhsRelation.getIndex();
        int rhsRel = rhsRelation.getIndex();
        long pairKey = ((long) lhsRel << 32) | rhsRel;

        computedPairs.computeIfAbsent(pairKey, k -> {
            computePair(lhsRelation, rhsRelation);
            return Boolean.TRUE;
        });
    }

    private void computePair(Relation lhsRelation, Relation rhsRelation) {
        int lhsRel = lhsRelation.getIndex();
        int rhsRel = rhsRelation.getIndex();

        log.debug("Computing Unary INDs for Lhs Rel:{}, Rhs Rel: {}", lhsRel, rhsRel);

        int lhsNumCols = lhsRelation.getNumOfAttributes();
        int rhsNumCols = rhsRelation.getNumOfAttributes();

        Map<String, BitSet> invertedRhs = rhsRelation.getInvertedAttributeValues();

        for (int lc = 0; lc < lhsNumCols; lc++) {
            String[] lhsVals = lhsRelation.getSortedAttributeSet()[lc];

            BitSet candidates = new BitSet();
            candidates.set(0, rhsNumCols);

            // prune by size and same col
            for (int rc = candidates.nextSetBit(0); rc >= 0; rc = candidates.nextSetBit(rc + 1)) {
                if (lhsRel == rhsRel && lc == rc) {
                    candidates.clear(rc);
                    continue;
                }

                if (rhsRelation.getSortedAttributeSet()[rc].length < lhsVals.length){
                    candidates.clear(rc);
                }
            }

            if (candidates.isEmpty()){
                continue;
            }

            for (String v : lhsVals) {
                if (candidates.isEmpty()) break;
                BitSet rhsColsWithV = invertedRhs.get(v);
                if (rhsColsWithV == null || rhsColsWithV.isEmpty()) {
                    candidates.clear();
                    break;
                }
                candidates.and(rhsColsWithV);
            }

            for (int rc = candidates.nextSetBit(0); rc >= 0; rc = candidates.nextSetBit(rc + 1)) {
                add(lhsRel, lc, rhsRel, rc);
            }
        }
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"unaryINDs\": {\n");

        // group by (lhsRel, rhsRel) pair
        boolean firstPair = true;
        for (Map.Entry<Integer, Map<Integer, Map<Integer, BitSet>>> lhsEntry : forward.entrySet()) {
            int lhsRel = lhsEntry.getKey();

            for (Map.Entry<Integer, Map<Integer, BitSet>> rhsRelEntry : lhsEntry.getValue().entrySet()) {
                int rhsRel = rhsRelEntry.getKey();

                if (!firstPair) sb.append(",\n");
                sb.append("    \"R").append(lhsRel).append("->R").append(rhsRel).append("\": [\n");

                // list all col bindings for this pair
                boolean firstBinding = true;
                for (Map.Entry<Integer, BitSet> rhsColEntry : rhsRelEntry.getValue().entrySet()) {
                    int rhsCol = rhsColEntry.getKey();
                    BitSet lhsCols = rhsColEntry.getValue();

                    for (int lhsCol = lhsCols.nextSetBit(0); lhsCol >= 0; lhsCol = lhsCols.nextSetBit(lhsCol + 1)) {
                        if (!firstBinding) sb.append(",\n");
                        sb.append("      \"").append(lhsCol)
                                .append("_").append(rhsCol).append("\"");
                        firstBinding = false;
                    }
                }

                sb.append("\n    ]");
                firstPair = false;
            }
        }

        sb.append("\n  }\n}");
        return sb.toString();
    }
}
