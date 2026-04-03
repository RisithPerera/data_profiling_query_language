package de.metathesis;

import de.metathesis.structures.AttributeRepresentative;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import lombok.Getter;

import java.util.*;

public class Sampler {
    @Getter
    private final Relation relation;

    private float efficiencyThreshold = 0.01f; // 1% Efficiency
    private final float memoryThreshold = 0.8f; // 80% Memory Threshold

    private final PriorityQueue<AttributeRepresentative> samplingQueue = new PriorityQueue<>();

    @Getter
    private final Map<Integer, List<BitSet>> negativeCover = new HashMap<>();

    @Getter
    private final Map<Integer, List<BitSet>> positiveCover = new HashMap<>();

    public Sampler(Relation relation) {
        this.relation = relation;
    }

    public void init() {
        // Initialize covers
        for (int rhs = 0; rhs < this.relation.getNumOfAttributes(); rhs++) {
            this.negativeCover.put(rhs, new ArrayList<>());
            List<BitSet> candidates = new ArrayList<>();
            candidates.add(new BitSet(this.relation.getNumOfAttributes()));
            this.positiveCover.put(rhs, candidates);
        }

        for (PositionListIndex pli : this.relation.getUnaryPLIs()) {
            if (pli.getClusters().isEmpty()){
                continue; // unique column → skip
            }

            samplingQueue.add(new AttributeRepresentative(pli, this.relation.getNumOfAttributes()));
        }
    }

    public Map<Integer, List<BitSet>> start() {
        while (!samplingQueue.isEmpty() && (this.samplingQueue.peek().getEfficiency() >= this.efficiencyThreshold)) {
            AttributeRepresentative ar = samplingQueue.poll();
            ar.runNext(this.relation.getCompressedRecords(), this.negativeCover, this.positiveCover);

            float efficiency = ar.getEfficiency();
            System.out.printf("Attribute: %d, WindowDistance: %d, Efficiency: %f, Threshold: %f\n", ar.getAttributeIndex(), ar.getWindowDistance(), efficiency, this.efficiencyThreshold);

            // Re-insert if not exhausted
            if (!ar.isExhausted() && ar.getEfficiency() > 0.0f) {
                samplingQueue.add(ar);
            }

            if (!this.samplingQueue.isEmpty()) {
                // This is an optimization that we added after writing the HyFD paper
                this.efficiencyThreshold = Math.min(this.efficiencyThreshold / 2, this.samplingQueue.peek().getEfficiency() * 0.9f);
            }

            // Memory check
            if (MemoryUtils.systemMemoryUsage() > this.memoryThreshold) {
                System.out.println("Memory threshold reached, stopping sampling!");
                break;
            }
        }

        return this.positiveCover;
    }
}
