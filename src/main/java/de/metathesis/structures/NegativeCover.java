package de.metathesis.structures;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class NegativeCover {
    @Getter
    private final List<ObjectOpenHashSet<BitSet>> levels = new ArrayList<>();

    @Getter
    private final int numAttributes;

    public NegativeCover(int numAttributes) {
        this.numAttributes = numAttributes;
    }

    public boolean add(BitSet equalAttrs) {
        int card = equalAttrs.cardinality();

        while (levels.size() <= card){
            levels.add(new ObjectOpenHashSet<>());
        }

        return levels.get(card).add((BitSet) equalAttrs.clone());
    }

    public boolean contains(BitSet equalAttrs) {
        int card = equalAttrs.cardinality();

        if (card >= levels.size()){
            return false;
        }

        return levels.get(card).contains(equalAttrs);
    }
}