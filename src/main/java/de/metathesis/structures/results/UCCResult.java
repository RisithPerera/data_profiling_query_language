package de.metathesis.structures.results;

import de.metathesis.structures.ImmutableBitSet;
import lombok.Getter;

@Getter
public class UCCResult {
    private final int relationIndex;
    private final ImmutableBitSet attributeIndexList;

    public UCCResult(int relationIndex, ImmutableBitSet attributeIndexList) {
        this.relationIndex = relationIndex;
        this.attributeIndexList = attributeIndexList;
    }

    private int computeHash() {
        int h = 17;
        h = 31 * h + relationIndex;
        h = 31 * h + attributeIndexList.hashCode();
        return h;
    }

    @Override
    public int hashCode() {
        return computeHash();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UCCResult other)) return false;

        return relationIndex == other.relationIndex
                && attributeIndexList.equals(other.attributeIndexList);
    }

    @Override
    public String toString() {
        return "(" + relationIndex + ":" + attributeIndexList + ")";
    }
}
