package de.metathesis;

import de.metathesis.structures.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import lombok.Getter;

import java.util.BitSet;
import java.util.PriorityQueue;
import java.util.Set;

public class Sampler2 {
    @Getter
    private final Relation relation;

    private float efficiencyThreshold = 0.01f;
    private final float memoryThreshold = 0.8f;
    private final PriorityQueue<AttributeRepresentative2> samplingQueue = new PriorityQueue<>();
    private boolean isInitialSampling = true;

    @Getter
    private final NegativeCover negCover;

    public Sampler2(Relation relation) {
        this.relation = relation;
        this.negCover = new NegativeCover(relation.getNumOfAttributes());
    }

    public NegativeCover run(Set<IntIntImmutablePair> comparisonSuggestions) {
        int numAttributes = relation.getNumOfAttributes();
        int[][] compressedRecords = relation.getCompressedRecords();
        NegativeCover newNonFds = new NegativeCover(numAttributes);

        if (!comparisonSuggestions.isEmpty()) {
            BitSet agree = new BitSet(numAttributes);
            for (IntIntImmutablePair pair : comparisonSuggestions) {
                int r1 = pair.leftInt();
                int r2 = pair.rightInt();

                Utility.match(agree, compressedRecords[r1], compressedRecords[r2]);

                if (!agree.isEmpty() && negCover.add(agree)) {
                    newNonFds.add(agree);
                }
            }
        }

        if (isInitialSampling) {
            System.out.println(relation.getIndex() +":"+relation.getRelationalInput().relationName());
            ClusterComparator comparator = new ClusterComparator(compressedRecords, compressedRecords[0].length - 1, 1);
            for (PositionListIndex pli : relation.getUnaryPLIs()) {
                for (IntArrayList cluster : pli.getClusters()) {
                    cluster.sort(comparator);
                }
                comparator.incrementActiveKey();
            }

            for (PositionListIndex pli : relation.getUnaryPLIs()) {
//                if (pli.isUnique() || pli.isConstant()) {
//                    continue;
//                }

                AttributeRepresentative2 rep = new AttributeRepresentative2(pli, numAttributes);
                rep.runNext(compressedRecords, negCover, newNonFds);

                if (!rep.isExhausted() && rep.getEfficiency() > 0.0f) {
                    samplingQueue.add(rep);
                }
            }

            if (!samplingQueue.isEmpty()) {
                efficiencyThreshold = Math.min(0.01f, samplingQueue.peek().getEfficiency() * 0.5f);
            }

            isInitialSampling = false;
        } else {
            if (!samplingQueue.isEmpty()) {
                efficiencyThreshold = Math.min(efficiencyThreshold / 2, samplingQueue.peek().getEfficiency() * 0.9f);
            }
        }

        while (!samplingQueue.isEmpty() && samplingQueue.peek().getEfficiency() >= efficiencyThreshold) {
            AttributeRepresentative2 rep = samplingQueue.poll();
            rep.runNext(compressedRecords, negCover, newNonFds);

            if (!rep.isExhausted() && rep.getEfficiency() > 0.0f) {
                samplingQueue.add(rep);
            }

            if (MemoryUtils.systemMemoryUsage() > memoryThreshold) {
                break;
            }
        }

        return newNonFds;
    }
}