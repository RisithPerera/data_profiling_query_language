package de.metathesis.structures;

import de.metathesis.Utility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.*;

public class AttributeRepresentative implements Comparable<AttributeRepresentative> {
    @Getter
    private final int relationIndex;

    @Getter
    private final int attributeIndex;
    private final int numOfAttributes;
    private final List<IntArrayList> clusters;
    private final BitSet agree;

    @Getter
    private int windowDistance = 0; //Unsafe
    private int numNewViolations = 0;
    private int numComparisons = 0;

    public AttributeRepresentative(PositionListIndex pli, int numOfAttributes) {
        this.relationIndex = pli.getAttributeSet().getRelationIndex();
        this.attributeIndex = pli.getAttributeSet().getAttributeIndexSet().nextSetBit(0);
        this.clusters = new ArrayList<>(pli.getClusters()); //Create a shallow copy of cluster
        this.numOfAttributes = numOfAttributes;
        this.agree = new BitSet(numOfAttributes);
    }

    public float getEfficiency() {
        if (numComparisons == 0) return 1.0f;

        return (float) numNewViolations / numComparisons;
    }

    @Override
    public int compareTo(AttributeRepresentative o) {
        return Float.compare(o.getEfficiency(), this.getEfficiency());
    }

    public void runNext(int[][] compressedRecords, Map<Integer, List<BitSet>> negativeCover, Map<Integer, List<BitSet>> positiveCover) {
        this.windowDistance++;
        this.numNewViolations = 0; // reset in each round
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

                agree.clear();
                for (int col = 0; col < numOfAttributes; col++) {
                    if (compressedRecords[r1][col] != -1 && compressedRecords[r1][col] == compressedRecords[r2][col]) {
                        agree.set(col);
                    }
                }

                this.numComparisons++;
                if (agree.isEmpty()) continue;

                BitSet disagree = (BitSet) agree.clone();
                disagree.flip(0, numOfAttributes);

                for (int rhs = disagree.nextSetBit(0); rhs >= 0; rhs = disagree.nextSetBit(rhs + 1)) {
                    boolean isNew = Utility.addMaximal(negativeCover.get(rhs), agree);
                    if (isNew) {
                        positiveCover.put(rhs, specialize(negativeCover.get(rhs), rhs));
                        numNewViolations++;
                    }
                }

                System.out.printf("Attr: %d (%d, %d) Comp: %d, Violations: %d\n", this.attributeIndex, r1, r2, this.numComparisons, this.numNewViolations);
            }
        }
        System.out.println("------------------------");
    }

    public boolean isExhausted() {
        return clusters.isEmpty();
    }

    private List<BitSet> specialize(List<BitSet> violations, int rhs) {
        List<BitSet> candidates = new ArrayList<>();
        candidates.add(new BitSet(numOfAttributes));

        for (BitSet violation : violations) {
            List<BitSet> newCandidates = new ArrayList<>();

            for (BitSet candidate : candidates) {
                boolean isHit = Utility.isSubset(candidate, violation);

                if (!isHit) {
                    Utility.addMinimal(newCandidates, candidate);
                } else {
                    //add one attr from outside violation
                    for (int attr = 0; attr < numOfAttributes; attr++) {
                        if (attr == rhs){
                            continue;
                        }

                        if (violation.get(attr)){
                            continue;
                        }

                        BitSet specialized = (BitSet) candidate.clone();
                        specialized.set(attr);
                        Utility.addMinimal(newCandidates, specialized);
                    }
                }
            }

            candidates = newCandidates;
        }

        return candidates;
    }
}