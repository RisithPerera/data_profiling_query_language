package de.metathesis.structures.results;

import de.metathesis.structures.ImmutableBitSet;
import lombok.Getter;

@Getter
public class FDResult {
    private final int relationIndex;
    private final ImmutableBitSet lhsAttributeIndexList;
    private final ImmutableBitSet rhsAttributeIndexList;

    public FDResult(int relationIndex,
                    ImmutableBitSet lhsAttributeIndexList,
                    ImmutableBitSet rhsAttributeIndexList) {
        this.relationIndex = relationIndex;
        this.lhsAttributeIndexList = lhsAttributeIndexList;
        this.rhsAttributeIndexList = rhsAttributeIndexList;
    }

    private int computeHash() {
        int h = 17;
        h = 31 * h + relationIndex;
        h = 31 * h + lhsAttributeIndexList.hashCode();
        h = 31 * h + rhsAttributeIndexList.hashCode();
        return h;
    }

    @Override
    public int hashCode() {
        return computeHash();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FDResult other)) return false;

        return this.relationIndex == other.relationIndex
                && this.lhsAttributeIndexList.equals(other.lhsAttributeIndexList)
                && this.rhsAttributeIndexList.equals(other.rhsAttributeIndexList);
    }

    @Override
    public String toString() {
        return "(" + this.relationIndex + ":" + this.lhsAttributeIndexList + ")" +
                " -> " +
                "(" + this.relationIndex + ":" + this.rhsAttributeIndexList + ")";
    }
}
