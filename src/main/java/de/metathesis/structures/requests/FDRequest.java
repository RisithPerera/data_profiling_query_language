package de.metathesis.structures.requests;

import de.metathesis.structures.ImmutableBitSet;
import lombok.Getter;

@Getter
public class FDRequest {
    private final boolean isLocked;

    private int[] relations;
    private int level;
    private ImmutableBitSet[] combinations;

    public FDRequest(int[] relations, int level) {
        this.relations = relations;
        this.level = level;
        this.isLocked = false;
    }

    public FDRequest(ImmutableBitSet[] combinations) {
        this.combinations = combinations;
        this.isLocked = true;
    }
}
