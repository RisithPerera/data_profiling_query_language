package de.metathesis.structures;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class PositionListIndex {
    private final AttributeBitSet attributeSet;
    private final List<IntArrayList> clusters;

    @Setter
    private int numUniqueValues;

    @Setter
    private int numOfRecords;

    public PositionListIndex(AttributeBitSet attributeSet, List<IntArrayList> clusters) {
        this.attributeSet = attributeSet;
        this.clusters = clusters;
    }

    public boolean isUnique() {
        return this.clusters.isEmpty();
    }

    public boolean isConstant() {
        if (this.numOfRecords <= 1) {
            return true;
        }

        return (this.clusters.size() == 1) && (this.clusters.getFirst().size() == this.numOfRecords);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionListIndex other)) return false;

        return this.attributeSet.equals(other.attributeSet) && this.clusters.equals(other.clusters);
    }
}

