package de.metathesis.structures;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.*;

public class PositionListIndex {
    private final AttributeList attributes;
    private final List<IntArrayList> clusters; //Including single clusters

    public PositionListIndex(AttributeList attributes, List<IntArrayList> clusters) {
        this.attributes = attributes;
        this.clusters = clusters;
    }

    public AttributeList getAttributes() {
        return attributes;
    }

    public List<IntArrayList> getClusters() {
        return clusters;
    }

    public PositionListIndex intersect(PositionListIndex other) {
        List<IntArrayList> intersectClusters = new ArrayList<>();

        for (IntArrayList list1 : this.clusters) {
            for (IntArrayList list2 : other.clusters) {

                // Compute intersection of list1 and list2
                IntArrayList common = intersectCluster(list1, list2);

                if (!common.isEmpty()) {
                    intersectClusters.add(common);
                }
            }
        }

        return new PositionListIndex(this.attributes.union(other.attributes), intersectClusters);
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
}

