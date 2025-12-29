package de.metathesis.structures.requests;

import de.metathesis.structures.ImmutableBitSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class UCCRequest {
    private boolean isLocked = false;
    private IntArrayList relations;
    private int level;

    private ImmutableBitSet[] combinations;

    public UCCRequest(IntArrayList relations, int level) {
        this.relations = relations;
        this.level = level;
        this.isLocked = false;
    }

    public UCCRequest(ImmutableBitSet[] combinations) {
        this.combinations = combinations;
        this.isLocked = true;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public IntArrayList getRelations() {
        return relations;
    }

    public ImmutableBitSet[] getCombinations() {
        return combinations;
    }

    public int getLevel() {
        return level;
    }
}
