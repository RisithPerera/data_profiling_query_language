package de.metathesis.structures;

import de.metathesis.ProfilingContext;
import lombok.Getter;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Immutable representation of a set of integer indices using a BitSet.
 * Also keep separate array for keeping the original order for INDs.
 * Cached hash code for fast hash-based lookups and comparisons.
 * <p>
 * Author: Risith Perera
 * Date: 2025-12-29
 */
public class AttributeBitSet{
    @Getter
    private final int relationIndex;

    //TODO: May be use only array????
    private final BitSet attributeIndexSet;  //For set operations and equality check

    private final int[] attributeIndexArray; //To preserve the order

    @Getter
    private final int hashCode; //Cashing Hash for performance

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

    //TODO: This is temporary for testing purposes. Remove this!
    @Override
    public String toString() {
        Relation relation = ProfilingContext.getInstance().getRelation(this.relationIndex);

        String[] columns = Arrays.stream(this.attributeIndexArray)
                .mapToObj(i -> relation.getAttributeNames()[i])
                .toArray(String[]::new);

        return "(" + relation.getName() + ":" + Arrays.toString(columns) + ")";
    }

    //TODO: Use original one
    public String toStringOriginal() {
        return "(" + this.relationIndex + ":" + Arrays.toString(attributeIndexArray) + ")";
    }
}