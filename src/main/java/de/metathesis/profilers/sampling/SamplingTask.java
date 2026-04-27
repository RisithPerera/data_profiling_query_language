package de.metathesis.profilers.sampling;

import de.metathesis.profilers.structures.NegativeCover;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class SamplingTask implements Comparable<SamplingTask> {

    @Getter
    private final int attributeIndex;
    private final List<IntArrayList> clusters;
    private final BitSet agree;

    @Getter
    private int windowDistance = 0;
    private int numNewViolations = 0;
    private int numComparisons = 0;

    public SamplingTask(PositionListIndex pli, int numOfAttributes) {
        this.attributeIndex = pli.getAttributeSet().getAttributeIndexSet().nextSetBit(0);
        this.clusters = new ArrayList<>(pli.getClusters());
        this.agree = new BitSet(numOfAttributes);
    }

    public float getEfficiency() {
        if (numComparisons == 0) return 0.0f;
        return (float) numNewViolations / numComparisons;
    }

    @Override
    public int compareTo(SamplingTask o) {
        if (o == null) return -1;
        return Float.compare(o.getEfficiency(), this.getEfficiency());
    }

    public void runNext(int[][] compressedRecords, NegativeCover negCover, NegativeCover newNonFds) {
        this.windowDistance++;
        this.numNewViolations = 0;
        this.numComparisons = 0;

        Iterator<IntArrayList> clusterIterator = clusters.iterator();
        while (clusterIterator.hasNext()) {
            IntArrayList cluster = clusterIterator.next();
            if (cluster.size() <= this.windowDistance) {
                clusterIterator.remove();
                continue;
            }

            for (int i = 0; i < cluster.size() - this.windowDistance; i++) {
                int r1 = cluster.getInt(i);
                int r2 = cluster.getInt(i + this.windowDistance);

                Utility.match(agree, compressedRecords[r1], compressedRecords[r2]);

                this.numComparisons++;
                if (agree.isEmpty()) continue;

                if (negCover.add(agree)) {
                    newNonFds.add(agree);
                    this.numNewViolations++;
                }

                //System.out.printf("Attr: %d (%d, %d) Comp: %d, Violations: %d\n", this.attributeIndex, r1, r2, this.numComparisons, this.numNewViolations);
            }
        }
    }

    public boolean isExhausted() { return clusters.isEmpty(); }
}