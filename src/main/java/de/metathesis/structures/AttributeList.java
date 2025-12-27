package de.metathesis.structures;

import java.util.BitSet;

public final class AttributeList {
    private final BitSet bitSet;
    private final int hash;

    public AttributeList(BitSet src) {
        this.bitSet = (BitSet) src.clone();
        this.hash = bitSet.hashCode();
    }

    AttributeList union(AttributeList other) {
        BitSet out = this.getBitSet();
        out.or(other.bitSet);
        return new AttributeList(out);
    }

    public BitSet getBitSet() {
        return (BitSet) this.bitSet.clone();
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
