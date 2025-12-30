package de.metathesis.structures.requests;

import de.metathesis.structures.ImmutableBitSet;
import lombok.Getter;

@Getter
public class INDRequest {
    private final boolean isLocked;

    private int[] relations;
    private int level;
    private ImmutableBitSet[] combinations;

    public INDRequest(int[] relations, int level) {
        this.relations = relations;
        this.level = level;
        this.isLocked = false;
    }

    public INDRequest(ImmutableBitSet[] combinations) {
        this.combinations = combinations;
        this.isLocked = true;
    }
}
