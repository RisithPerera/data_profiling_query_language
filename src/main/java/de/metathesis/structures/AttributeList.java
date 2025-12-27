package de.metathesis.structures;

import java.util.BitSet;

public final class AttributeList {
    private final BitSet bitSet;
    private final int hash;

    AttributeList(BitSet src) {
        this.bitSet = (BitSet) src.clone();
        this.hash = bitSet.hashCode();
    }

    AttributeList union(AttributeList other) {
        BitSet out = (BitSet) this.bitSet.clone();
        out.or(other.bitSet);
        return new AttributeList(out);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AttributeList other && bitSet.equals(other.bitSet);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
