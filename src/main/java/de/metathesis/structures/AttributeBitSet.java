package de.metathesis.structures;

import de.metathesis.Preprocessor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Immutable representation of a set of integer indices using a BitSet.
 * Suitable for modeling column combinations (e.g., [2,3,6,8]) in lattice-based algorithms.
 * Cached hash code for fast hash-based lookups and comparisons.
 * <p>
 * Author: Risith Perera
 * Date: 2025-12-29
 */

public class AttributeBitSet{
    @Getter
    private final int relationIndex;

    private final BitSet attributeIndexSet;

    private final int[] attributeIndexArray; //For IND to with different permutations

    @Getter
    private final int hashCode; //Cashing Hash for performance

    private final Preprocessor preprocessor = Preprocessor.getInstance(); //This is temporary

    public AttributeBitSet(int relationIndex, int columnIndex) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = new BitSet();
        this.attributeIndexSet.set(columnIndex);
        this.attributeIndexArray = new int[1];
        this.attributeIndexArray[0] = columnIndex;
        this.hashCode = computeHash();
    }

    public AttributeBitSet(int relationIndex, int[] attributeIndexSetArray) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = new BitSet();

        for (int attr : attributeIndexSetArray) {
            this.attributeIndexSet.set(attr);
        }

        this.attributeIndexArray = attributeIndexSetArray.clone();
        this.hashCode = computeHash();
    }

    public AttributeBitSet(int relationIndex, BitSet attributeIndexSet) {
        this.relationIndex = relationIndex;
        this.attributeIndexSet = (BitSet) attributeIndexSet.clone();

        this.attributeIndexArray = new int[attributeIndexSet.cardinality()];
        int i = 0;
        for (int c = attributeIndexSet.nextSetBit(0); c >= 0; c = attributeIndexSet.nextSetBit(c + 1)) {
            attributeIndexArray[i++] = c;
        }

        this.hashCode = computeHash();
    }

    public BitSet getAttributeIndexSet() {
        return (BitSet) this.attributeIndexSet.clone();
    }

    public int[] getAttributeIndexArray() {
        return attributeIndexArray.clone();
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

    public AttributeBitSet difference(AttributeBitSet other) {
        if (this.relationIndex != other.relationIndex) {
            throw new UnsupportedOperationException("Cannot perform difference on AttributeBitSets from different relations");
        }

        BitSet out = this.getAttributeIndexSet();
        out.andNot(other.attributeIndexSet);

        return new AttributeBitSet(this.relationIndex, out);
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

        return new AttributeBitSet(relationIndex, projected);
    }

    public List<AttributeBitSet> immediateSubsets() {
        List<AttributeBitSet> subsets = new ArrayList<>();

        if (attributeIndexSet.cardinality() <= 1) {
            return subsets;
        }

        for (int bit = attributeIndexSet.nextSetBit(0); bit >= 0; bit = attributeIndexSet.nextSetBit(bit + 1)) {
            BitSet bs = (BitSet) attributeIndexSet.clone();
            bs.clear(bit);
            subsets.add(new AttributeBitSet(relationIndex, bs));
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

    //This is temporary for testing purposes
    @Override
    public String toString() {
        Relation relation = this.preprocessor.getRelation(this.relationIndex);

        String[] columns = Arrays.stream(this.attributeIndexArray)
                .mapToObj(i -> relation.getAttributeNames()[i])
                .toArray(String[]::new);

        return "(" + relation.getName() + ":" + Arrays.toString(columns) + ")";
    }

    public String toStringOriginal() {
        return "(" + this.relationIndex + ":" + attributeIndexSet.toString() + ")";
    }
}