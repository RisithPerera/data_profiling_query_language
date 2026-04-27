package de.metathesis.structures;

import de.metathesis.Preprocessor;
import lombok.Getter;

import java.util.Arrays;
import java.util.BitSet;

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