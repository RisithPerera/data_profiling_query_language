package de.metathesis.structures;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable representation of a set of integer indices using a BitSet.
 * Suitable for modeling column combinations (e.g., [2,3,6,8]) in lattice-based algorithms.
 * Cached hash code for fast hash-based lookups and comparisons.
 * <p>
 * Author: Risith Perera
 * Date: 2025-12-29
 */

public final class AttributeBitSet{
    @Getter
    private final int relationIndex;

    private final BitSet attributeIndexSet;

    @Getter
    private final int hashCode; //Cashing Hash for performance

    @Getter
    private final String relation; //Temporary Fields

    @Getter
    private final String[] attributeSet; //Temporary Fields

    public AttributeBitSet(int relationIndex, int columnIndex, String relation, String attribute) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = new BitSet();
        this.attributeIndexSet.set(columnIndex);
        this.hashCode = computeHash();

        this.relation = relation;
        this.attributeSet = new String[]{attribute};
    }

    public AttributeBitSet(int relationIndex, BitSet attributeIndexSet, String relation, String[] attributeList) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = (BitSet) attributeIndexSet.clone();
        this.hashCode = computeHash();

        this.relation = relation;
        this.attributeSet = attributeList;
    }

    public BitSet getAttributeIndexSet() {
        return (BitSet) this.attributeIndexSet.clone();
    }

    public AttributeBitSet union(AttributeBitSet other) {
        if (this.relationIndex != other.relationIndex) {
            throw new UnsupportedOperationException("Cannot perform union on AttributeBitSets from different relations");
        }

        BitSet out = this.getAttributeIndexSet();
        out.or(other.attributeIndexSet);

        return new AttributeBitSet(this.relationIndex, out, this.relation, new String[0]);
    }

    public AttributeBitSet intersect(AttributeBitSet other) {
        if (this.relationIndex != other.relationIndex) {
            throw new UnsupportedOperationException("Cannot perform intersect on AttributeBitSets from different relations");
        }

        BitSet out = this.getAttributeIndexSet();
        out.and(other.attributeIndexSet);
        return new AttributeBitSet(this.relationIndex, out, this.relation, new String[0]);
    }

    public AttributeBitSet difference(AttributeBitSet other) {
        if (this.relationIndex != other.relationIndex) {
            throw new UnsupportedOperationException("Cannot perform difference on AttributeBitSets from different relations");
        }

        BitSet out = this.getAttributeIndexSet();
        out.andNot(other.attributeIndexSet);

        return new AttributeBitSet(this.relationIndex, out, this.relation, new String[0]);
    }

    public boolean isSubsetOf(AttributeBitSet other) {
        if (this.relationIndex != other.relationIndex) {
            throw new UnsupportedOperationException("Cannot compare AttributeBitSets from different relations");
        }

        BitSet tmp = (BitSet) this.attributeIndexSet.clone();
        tmp.andNot(other.attributeIndexSet);
        return tmp.isEmpty();
    }

    public AttributeBitSet project(AttributeBitSet lhsSubset) {
        if (this.size() != lhsSubset.size() + 1) {
            throw new IllegalArgumentException("Projection requires RHS to be exactly one level higher than LHS subset");
        }

        BitSet projected = new BitSet();

        int lhsPos = 0;
        for (int bit = attributeIndexSet.nextSetBit(0); bit >= 0; bit = attributeIndexSet.nextSetBit(bit + 1)) {
            if (lhsSubset.attributeIndexSet.get(lhsPos)) {
                projected.set(bit);
            }
            lhsPos++;
        }

        return new AttributeBitSet(relationIndex, projected, this.relation, new String[0]);
    }

    public List<AttributeBitSet> immediateSubsets() {
        List<AttributeBitSet> subsets = new ArrayList<>();

        if (attributeIndexSet.cardinality() <= 1) {
            return subsets;
        }

        for (int bit = attributeIndexSet.nextSetBit(0); bit >= 0; bit = attributeIndexSet.nextSetBit(bit + 1)) {
            BitSet bs = (BitSet) attributeIndexSet.clone();
            bs.clear(bit);
            subsets.add(new AttributeBitSet(relationIndex, bs, this.relation, new String[0]));
        }

        return subsets;
    }

    public int size() {
        return attributeIndexSet.cardinality();
    }

    public boolean isEmpty() {
        return attributeIndexSet.isEmpty();
    }

    private int computeHash() {
        return 31 * this.attributeIndexSet.hashCode() + this.relationIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeBitSet other)) return false;

        return this.relationIndex == other.relationIndex
                && this.attributeIndexSet.equals(other.attributeIndexSet);
    }


    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "(" + this.relationIndex + ":" + this.attributeIndexSet.toString() + ") -> (" + this.relation + ":" + Arrays.toString(this.attributeSet) + ")";
    }
}
