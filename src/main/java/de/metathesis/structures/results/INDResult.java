package de.metathesis.structures.results;

import de.metathesis.structures.ImmutableBitSet;
import lombok.Getter;

@Getter
public class INDResult {
    private final int lhsRelationIndex;
    private final ImmutableBitSet lhsAttributeIndexList;

    private final int rhsRelationIndex;
    private final ImmutableBitSet rhsAttributeIndexList;

    public INDResult(int lhsRelationIndex, ImmutableBitSet lhsAttributeIndexList,
                     int rhsRelationIndex, ImmutableBitSet rhsAttributeIndexList) {
        assert (lhsAttributeIndexList.size() == rhsAttributeIndexList.size());

        this.lhsRelationIndex = lhsRelationIndex;
        this.lhsAttributeIndexList = lhsAttributeIndexList;

        this.rhsRelationIndex = rhsRelationIndex;
        this.rhsAttributeIndexList = rhsAttributeIndexList;
    }

    private int computeHash() {
        int h = 17;
        h = 31 * h + lhsRelationIndex;
        h = 31 * h + lhsAttributeIndexList.hashCode();
        h = 31 * h + rhsRelationIndex;
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
        if (!(obj instanceof INDResult other)) return false;

        return lhsRelationIndex == other.lhsRelationIndex
                && rhsRelationIndex == other.rhsRelationIndex
                && lhsAttributeIndexList.equals(other.lhsAttributeIndexList)
                && rhsAttributeIndexList.equals(other.rhsAttributeIndexList);
    }

    @Override
    public String toString() {
        return "(" + lhsRelationIndex + ":" + lhsAttributeIndexList + ")" +
                " C " +
                "(" + rhsRelationIndex + ":" + rhsAttributeIndexList + ")";
    }
}
