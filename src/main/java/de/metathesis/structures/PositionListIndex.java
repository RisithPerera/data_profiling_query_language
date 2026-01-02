package de.metathesis.structures;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PositionListIndex {
    private final AttributeBitSet attributeSet;
    private final List<IntArrayList> clusters; //Including single clusters

    public PositionListIndex(AttributeBitSet attributeSet, List<IntArrayList> clusters) {
        this.attributeSet = attributeSet;
        this.clusters = clusters;
    }

    public boolean isUnique() {
        return this.clusters.isEmpty();
    }

    public PositionListIndex intersect(PositionListIndex other) {
        List<IntArrayList> intersectClusters = new ArrayList<>();

        for (IntArrayList list1 : this.clusters) {
            for (IntArrayList list2 : other.clusters) {

                // Compute intersection of list1 and list2
                IntArrayList common = intersectCluster(list1, list2);

                if (common.size() > 1) {
                    intersectClusters.add(common);
                }
            }
        }

        return new PositionListIndex(this.attributeSet.union(other.attributeSet), intersectClusters);
    }

    private IntArrayList intersectCluster(IntArrayList list1, IntArrayList list2) {
        IntArrayList res = new IntArrayList();

        int i = 0;
        int j = 0;

        while (i < list1.size() && j < list2.size()) {
            int a = list1.getInt(i);
            int b = list2.getInt(j);

            if (a == b) {
                res.add(a);
                i++;
                j++;
            } else if (a < b) {
                i++;
            } else {
                j++;
            }
        }

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionListIndex other)) return false;

        return this.attributeSet.equals(other.attributeSet);
    }
}

