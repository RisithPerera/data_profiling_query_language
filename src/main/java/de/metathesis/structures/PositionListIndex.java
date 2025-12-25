package de.metathesis.structures;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.*;

public class PositionListIndex {

    private final TreeMap<String, IntArrayList> clusters; //Including single clusters

    PositionListIndex(TreeMap<String, IntArrayList>  clusters) {
        this.clusters = clusters;
    }

    public TreeMap<String, IntArrayList> getClusters() {
        return clusters;
    }

    public PositionListIndex intersect(PositionListIndex other) {
        TreeMap<String, IntArrayList> intersectClusters = new TreeMap<>();

        for (Map.Entry<String, IntArrayList> entry1 : this.clusters.entrySet()) {
            IntArrayList list1 = entry1.getValue();
            for (Map.Entry<String, IntArrayList> entry2 : other.clusters.entrySet()) {
                IntArrayList list2 = entry2.getValue();

                // Compute intersection of list1 and list2
                IntArrayList common = intersectCluster(list1, list2);

                if (!common.isEmpty()) {
                    // Combine keys to form a new key
                    String newKey = entry1.getKey() + "_" + entry2.getKey();
                    intersectClusters.put(newKey, common);
                }
            }
        }

        return new PositionListIndex(intersectClusters);
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

