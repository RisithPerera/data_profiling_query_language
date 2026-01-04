package de.metathesis.structures;

import lombok.Getter;

import java.util.BitSet;

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

    public AttributeBitSet(int relationIndex, int columnIndex) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = new BitSet();
        this.attributeIndexSet.set(columnIndex);
        this.hashCode = computeHash();
    }

    public AttributeBitSet(int relationIndex, BitSet attributeIndexSet) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = (BitSet) attributeIndexSet.clone();
        this.hashCode = computeHash();
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
        return new AttributeBitSet(this.relationIndex, out);
    }

    public AttributeBitSet intersect(AttributeBitSet other) {
        if (this.relationIndex != other.relationIndex) {
            throw new UnsupportedOperationException("Cannot perform intersect on AttributeBitSets from different relations");
        }

        BitSet out = this.getAttributeIndexSet();
        out.and(other.attributeIndexSet);
        return new AttributeBitSet(this.relationIndex, out);
    }

    public int size() {
        return attributeIndexSet.cardinality();
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
        return "(" + this.relationIndex + ":" + attributeIndexSet.toString() + ")";
    }
}
