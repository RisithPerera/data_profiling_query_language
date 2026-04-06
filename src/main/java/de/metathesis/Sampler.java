package de.metathesis;

import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.AttributeRepresentative;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.Relation;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Sampler {
    @Getter
    private final Relation relation;

    private float efficiencyThreshold = 0.01f; // 1% efficiency
    private final float memoryThreshold = 0.8f; // 80% Memory Threshold

    private final PriorityQueue<AttributeRepresentative> samplingQueue = new PriorityQueue<>();

    @Getter
    private final Map<Integer, List<BitSet>> negativeCover = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, List<BitSet>> positiveCover = new ConcurrentHashMap<>();

    private boolean isInitialSampling = true;

    public Sampler(Relation relation) {
        this.relation = relation;
        int numOfAttributes = this.relation.getNumOfAttributes();

        // Initialize covers
        for (int attributeIndex = 0; attributeIndex < numOfAttributes; attributeIndex++) {
            this.negativeCover.put(attributeIndex, new ArrayList<>());
            this.positiveCover.put(attributeIndex, new ArrayList<>());
        }

        // Handle trivial cases from unary PLIs before sampling
        for (int attributeIndex = 0; attributeIndex < numOfAttributes; attributeIndex++) {
            PositionListIndex pli = relation.getUnaryPLIs()[attributeIndex];

            if (pli.isConstant(this.relation.getNumOfRecords())) {
                // If the column is constant, It is determined by all other columns
                BitSet emptyLhsCandidate = new BitSet(numOfAttributes);
                Utility.addMinimal(this.positiveCover.get(attributeIndex), emptyLhsCandidate);
            } else if (pli.isUnique()) {
                // If the column is unique, it determines all other columns
                for (int rhsAttributeIndex = 0; rhsAttributeIndex < numOfAttributes; rhsAttributeIndex++) {
                    if (rhsAttributeIndex == attributeIndex) continue;

                    BitSet singleLhsCandidate = new BitSet(numOfAttributes);
                    singleLhsCandidate.set(attributeIndex);
                    Utility.addMinimal(this.positiveCover.get(rhsAttributeIndex), singleLhsCandidate);
                }
            } else {
                // Normal column initialize with full complement
                BitSet fullLhsCandidate = new BitSet(numOfAttributes);
                fullLhsCandidate.set(0, numOfAttributes);
                fullLhsCandidate.clear(attributeIndex);
                Utility.addMinimal(this.positiveCover.get(attributeIndex), fullLhsCandidate);
            }
        }
    }

    public Map<Integer, List<BitSet>> run() {
        if(isInitialSampling){
            for (PositionListIndex pli : this.relation.getUnaryPLIs()) {
                if(pli.isUnique() || pli.isConstant(this.relation.getNumOfRecords())){
                    continue;
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
