package de.metathesis.structures;

import java.util.BitSet;

/**
 * Immutable representation of a set of integer indices using a BitSet.
 * Suitable for modeling column combinations (e.g., [2,3,6,8]) in lattice-based algorithms.
 * Cached hash code for fast hash-based lookups and comparisons.
 * <p>
 * Author: Risith Perera
 * Date: 2025-12-29
 */

public final class ImmutableBitSet {
    private final BitSet bitSet;
    private final int hash;

    public ImmutableBitSet(int columnIndex) {
        this.bitSet = new BitSet();
        this.bitSet.set(columnIndex);
        this.hash = this.bitSet.hashCode();
    }

    public ImmutableBitSet(BitSet bitSet) {
        this.bitSet = (BitSet) bitSet.clone();
        this.hash = bitSet.hashCode(); //Cashed the hash
    }

    ImmutableBitSet union(ImmutableBitSet other) {
        BitSet out = this.getBitSet();
        out.or(other.bitSet);
        return new ImmutableBitSet(out);
    }

    public int size() {
        return bitSet.cardinality();
    }

    public BitSet getBitSet() {
        return (BitSet) this.bitSet.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ImmutableBitSet other && bitSet.equals(other.bitSet);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return bitSet.toString();
    }
}
