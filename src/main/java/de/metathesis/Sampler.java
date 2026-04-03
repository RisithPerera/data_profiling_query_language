package de.metathesis;

import de.metathesis.structures.AttributeRepresentative;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import lombok.Getter;

import java.util.*;

public class Sampler {
    @Getter
    private final Relation relation;

    private float efficiencyThreshold = 0.01f; // 1% efficiency
    private final float memoryThreshold = 0.8f; // 80% Memory Threshold

    private final PriorityQueue<AttributeRepresentative> samplingQueue = new PriorityQueue<>();

    @Getter
    private final Map<Integer, List<BitSet>> negativeCover = new HashMap<>();

    @Getter
    private final Map<Integer, List<BitSet>> positiveCover = new HashMap<>();

    private boolean isInitialSampling = true;

    public Sampler(Relation relation) {
        this.relation = relation;

        // Initialize covers
        for (int rhs = 0; rhs < this.relation.getNumOfAttributes(); rhs++) {
            this.negativeCover.put(rhs, new ArrayList<>());
            List<BitSet> candidates = new ArrayList<>();
            candidates.add(new BitSet(this.relation.getNumOfAttributes()));
            this.positiveCover.put(rhs, candidates);
        }
    }

    public Map<Integer, List<BitSet>> run() {
        if(isInitialSampling){
            for (PositionListIndex pli : this.relation.getUnaryPLIs()) {
                if (pli.getClusters().isEmpty()){
                    continue; // unique column -> skip
                }
                AttributeRepresentative representer = new AttributeRepresentative(pli, this.relation.getNumOfAttributes());
                representer.runNext(this.relation.getCompressedRecords(), this.negativeCover, this.positiveCover);

                if (!representer.isExhausted() && representer.getEfficiency() > 0.0f) {
                    samplingQueue.add(representer);
                }
            }

            if (!this.samplingQueue.isEmpty()) {
                // This is an optimization that we added after writing the HyFD paper
                this.efficiencyThreshold = Math.min(0.01f, this.samplingQueue.peek().getEfficiency() * 0.5f);
            }

            isInitialSampling = false;
        }else{
            if (!this.samplingQueue.isEmpty()) {
                // This is an optimization that we added after writing the HyFD paper
                this.efficiencyThreshold = Math.min(this.efficiencyThreshold / 2, this.samplingQueue.peek().getEfficiency() * 0.9f);
            }
        }

        while (!samplingQueue.isEmpty() && (this.samplingQueue.peek().getEfficiency() >= this.efficiencyThreshold)) {
            AttributeRepresentative representer = samplingQueue.poll();
            representer.runNext(this.relation.getCompressedRecords(), this.negativeCover, this.positiveCover);

            System.out.printf("Attribute: %d, WindowDistance: %d, Efficiency: %f, Threshold: %f\n",
                    representer.getAttributeIndex(),
                    representer.getWindowDistance(),
                    representer.getEfficiency(),
                    this.efficiencyThreshold);

            // Re insert if not exhausted
            if (!representer.isExhausted() && representer.getEfficiency() > 0.0f) {
                samplingQueue.add(representer);
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
