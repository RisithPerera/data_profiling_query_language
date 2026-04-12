package de.metathesis;

import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

public class Validator {
    private final int numAttributes;
    private final int[][] compressed;
    private final PositionListIndex[] plis;
    private final FDTree posCover;
    private final Map<BitSet, BitSet> validatedFDs = new HashMap<>();

    private static final float THRESHOLD = 0.01f;

    public Validator(Relation relation) {
        this.numAttributes = relation.getNumOfAttributes();
        this.compressed = relation.getCompressedRecords();
        this.plis = relation.getUnaryPLIs();
        this.posCover = new FDTree(numAttributes);
        posCover.root.fds.set(0, numAttributes);

        for (int lhsAttr = 0; lhsAttr < numAttributes; lhsAttr++) {
            if (!relation.getUnaryPLIs()[lhsAttr].isUnique()) {
                continue;
            }
            for (int rhsAttr = 0; rhsAttr < numAttributes; rhsAttr++) {
                if (rhsAttr == lhsAttr) {
                    continue;
                }
                BitSet lhs = new BitSet(numAttributes);
                lhs.set(lhsAttr);
                if (!hasGeneralization(lhs, rhsAttr)) {
                    posCover.getOrCreate(lhs).fds.set(rhsAttr);
                }
            }
        }
    }

    public List<IntIntImmutablePair> validate(FDSet newNonFds, int targetLevel) {
        // Collect and snapshot BEFORE induction
        Map<BitSet, FDTreeNode> levelNodes = new LinkedHashMap<>();
        collectLevel(posCover.root, new BitSet(numAttributes), 0, targetLevel, levelNodes);

        Map<BitSet, BitSet> snapshot = new LinkedHashMap<>();
        for (Map.Entry<BitSet, FDTreeNode> entry : levelNodes.entrySet()) {
            if (!entry.getValue().fds.isEmpty()) {
                snapshot.put(entry.getKey(), (BitSet) entry.getValue().fds.clone());
            }
        }

        // Induct AFTER snapshot
        induct(newNonFds);

        if (snapshot.isEmpty()) {
            return null;
        }

        List<IntIntImmutablePair> suggestions = new ArrayList<>();
        List<BitSet> invalidLhs = new ArrayList<>();
        IntArrayList invalidRhs = new IntArrayList();
        int numValid = 0;
        int numInvalid = 0;

        for (Map.Entry<BitSet, BitSet> entry : snapshot.entrySet()) {
            BitSet lhs = entry.getKey();
            BitSet rhss = entry.getValue();
            FDTreeNode node = levelNodes.get(lhs);

            if (targetLevel == 0) {
                for (int rhs = rhss.nextSetBit(0); rhs >= 0;
                     rhs = rhss.nextSetBit(rhs + 1)) {
                    if (plis[rhs].isConstant()) {
                        numValid++;
                        validatedFDs.computeIfAbsent((BitSet) lhs.clone(), k -> new BitSet(numAttributes)).set(rhs);
                    } else {
                        node.fds.clear(rhs);
                        invalidLhs.add(lhs);
                        invalidRhs.add(rhs);
                        numInvalid++;
                    }
                }
            } else if (targetLevel == 1) {
                int lhsAttr = lhs.nextSetBit(0);
                for (int rhs = rhss.nextSetBit(0); rhs >= 0;
                     rhs = rhss.nextSetBit(rhs + 1)) {
                    if (refinesUnary(lhsAttr, rhs)) {
                        numValid++;
                        validatedFDs.computeIfAbsent((BitSet) lhs.clone(), k -> new BitSet(numAttributes)).set(rhs);
                    } else {
                        node.fds.clear(rhs);
                        invalidLhs.add(lhs);
                        invalidRhs.add(rhs);
                        numInvalid++;
                    }
                }
            } else {
                int firstAttr = lhs.nextSetBit(0);
                lhs.clear(firstAttr);
                BitSet validRhs = refinesMulti(firstAttr, lhs, rhss, suggestions);
                lhs.set(firstAttr);
                node.fds = validRhs;
                numValid += validRhs.cardinality();
                for (int rhs = validRhs.nextSetBit(0); rhs >= 0; rhs = validRhs.nextSetBit(rhs + 1)) {
                     validatedFDs.computeIfAbsent((BitSet) lhs.clone(), k -> new BitSet(numAttributes)).set(rhs);
                }

                BitSet invalid = (BitSet) rhss.clone();
                invalid.andNot(validRhs);
                for (int rhs = invalid.nextSetBit(0); rhs >= 0;
                     rhs = invalid.nextSetBit(rhs + 1)) {
                    invalidLhs.add(lhs);
                    invalidRhs.add(rhs);
                    numInvalid++;
                }
            }
        }

        // Specialize invalids into targetLevel+1
        for (int i = 0; i < invalidLhs.size(); i++) {
            BitSet lhs = invalidLhs.get(i);
            int rhs = invalidRhs.getInt(i);
            for (int attr = 0; attr < numAttributes; attr++) {
                if (lhs.get(attr) || attr == rhs) {
                    continue;
                }
                BitSet newLhs = (BitSet) lhs.clone();
                newLhs.set(attr);
                if (hasGeneralization(newLhs, rhs)) {
                    continue;
                }
                posCover.getOrCreate(newLhs).fds.set(rhs);
            }
        }

        if (numInvalid > numValid * THRESHOLD) {
            return suggestions;
        }

        return null;
    }

    public Map<Integer, List<BitSet>> results(int targetLevel) {
        Map<Integer, List<BitSet>> out = new HashMap<>();
        collectResults(posCover.root, new BitSet(numAttributes), targetLevel, 0, out);
        return out;
    }

    // --- Induction ---

    private void induct(FDSet newNonFds) {
        List<ObjectOpenHashSet<BitSet>> levels = newNonFds.getFdLevels();

        for (int i = levels.size() - 1; i >= 0; i--) {

            for (BitSet lhs : levels.get(i)) {
                BitSet rhs = (BitSet) lhs.clone();
                rhs.flip(0, numAttributes);
                for (int rhsAttr = rhs.nextSetBit(0); rhsAttr >= 0;
                     rhsAttr = rhs.nextSetBit(rhsAttr + 1)) {
                    specialize(lhs, rhsAttr);
                }
            }
        }
    }

    private void specialize(BitSet lhs, int rhs) {
        List<BitSet> generals = new ArrayList<>();
        collectGeneralizations(posCover.root, lhs, rhs,
                new BitSet(numAttributes), generals);
        for (BitSet gen : generals) {
            // Never remove already validated FDs
            BitSet confirmed = validatedFDs.get(gen);
            if (confirmed != null && confirmed.get(rhs)) {
                continue;
            }
            FDTreeNode node = posCover.get(gen);
            if (node != null) {
                node.fds.clear(rhs);
            }
            for (int attr = numAttributes - 1; attr >= 0; attr--) {
                if (lhs.get(attr) || attr == rhs) {
                    continue;
                }
                gen.set(attr);
                if (!hasGeneralization(gen, rhs)) {
                    posCover.getOrCreate(gen).fds.set(rhs);
                }
                gen.clear(attr);
            }
        }
    }

    // --- Tree traversal ---

    private void collectLevel(FDTreeNode node, BitSet currentLhs,
                              int currentDepth, int targetDepth,
                              Map<BitSet, FDTreeNode> out) {
        if (currentDepth == targetDepth) {
            if (!node.fds.isEmpty()) {
                out.put((BitSet) currentLhs.clone(), node);
            }
            return;
        }
        if (node.children == null) {
            return;
        }
        for (int attr = 0; attr < numAttributes; attr++) {
            if (node.children[attr] == null) {
                continue;
            }
            currentLhs.set(attr);
            collectLevel(node.children[attr], currentLhs,
                    currentDepth + 1, targetDepth, out);
            currentLhs.clear(attr);
        }
    }

    private void collectResults(FDTreeNode node, BitSet lhs,
                                int targetLevel, int currentDepth,
                                Map<Integer, List<BitSet>> out) {
        if (currentDepth == targetLevel) {
            for (int rhs = node.fds.nextSetBit(0); rhs >= 0;
                 rhs = node.fds.nextSetBit(rhs + 1)) {
                out.computeIfAbsent(rhs, k -> new ArrayList<>())
                        .add((BitSet) lhs.clone());
            }
            return;
        }
        if (node.children == null) {
            return;
        }
        for (int attr = 0; attr < numAttributes; attr++) {
            if (node.children[attr] == null) {
                continue;
            }
            lhs.set(attr);
            collectResults(node.children[attr], lhs, targetLevel, currentDepth + 1, out);
            lhs.clear(attr);
        }
    }

    private void collectGeneralizations(FDTreeNode node, BitSet lhs, int rhs,
                                        BitSet current, List<BitSet> out) {
        if (node.fds.get(rhs)) {
            out.add((BitSet) current.clone());
        }
        if (node.children == null) {
            return;
        }
        for (int attr = lhs.nextSetBit(0); attr >= 0;
             attr = lhs.nextSetBit(attr + 1)) {
            if (node.children[attr] == null) {
                continue;
            }
            current.set(attr);
            collectGeneralizations(node.children[attr], lhs, rhs, current, out);
            current.clear(attr);
        }
    }

    private boolean hasGeneralization(BitSet lhs, int rhs) {
        return checkGen(posCover.root, lhs, rhs, 0);
    }

    private boolean checkGen(FDTreeNode node, BitSet lhs, int rhs, int from) {
        if (node.fds.get(rhs)) {
            return true;
        }
        if (node.children == null) {
            return false;
        }
        for (int attr = lhs.nextSetBit(from); attr >= 0;
             attr = lhs.nextSetBit(attr + 1)) {
            if (node.children[attr] != null
                    && checkGen(node.children[attr], lhs, rhs, attr + 1)) {
                return true;
            }
        }
        return false;
    }

    // --- Refines ---

    private boolean refinesUnary(int lhsAttr, int rhs) {
        for (IntArrayList cluster : plis[lhsAttr].getClusters()) {
            int expected = compressed[cluster.getInt(0)][rhs];
            if (expected == -1) {
                return false;
            }
            for (int rec : cluster) {
                if (compressed[rec][rhs] != expected) {
                    return false;
                }
            }
        }
        return true;
    }

    private BitSet refinesMulti(int firstAttr, BitSet remainingLhs, BitSet rhs, List<IntIntImmutablePair> suggestions) {
        BitSet validRhs = (BitSet) rhs.clone();
        int[] rhsAttrs = rhs.stream().toArray();

        for (IntArrayList cluster : plis[firstAttr].getClusters()) {
            Long2IntOpenHashMap representative = new Long2IntOpenHashMap();
            Long2ObjectOpenHashMap<int[]> seen = new Long2ObjectOpenHashMap<>();

            for (int rec : cluster) {
                long key = clusterKey(remainingLhs, rec);
                if (key == Long.MIN_VALUE) {
                    continue;
                }

                if (seen.containsKey(key)) {
                    int[] existing = seen.get(key);
                    for (int r = validRhs.nextSetBit(0); r >= 0;
                         r = validRhs.nextSetBit(r + 1)) {
                        int idx = 0;
                        while (rhsAttrs[idx] != r) {
                            idx++;
                        }
                        if (compressed[rec][r] == -1 || compressed[rec][r] != existing[idx]) {
                            suggestions.add(new IntIntImmutablePair(rec, representative.get(key)));
                            validRhs.clear(r);
                            if (validRhs.isEmpty()) {
                                return validRhs;
                            }
                        }
                    }
                } else {
                    int[] vals = new int[rhsAttrs.length];
                    for (int i = 0; i < rhsAttrs.length; i++) {
                        vals[i] = compressed[rec][rhsAttrs[i]];
                    }
                    seen.put(key, vals);
                    representative.put(key, rec);
                }
            }
        }
        return validRhs;
    }

    private long clusterKey(BitSet lhs, int rec) {
        long key = 0;
        int shift = 0;
        for (int attr = lhs.nextSetBit(0); attr >= 0;
             attr = lhs.nextSetBit(attr + 1)) {
            int v = compressed[rec][attr];
            if (v == -1) {
                return Long.MIN_VALUE;
            }
            key |= ((long) v << shift);
            shift += 21;
        }
        return key;
    }
}