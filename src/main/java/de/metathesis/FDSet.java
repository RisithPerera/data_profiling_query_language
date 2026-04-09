package de.metathesis;

import lombok.Getter;

import java.util.*;

public class FDSet {
    @Getter
    private final List<Set<BitSet>> fdLevels = new ArrayList<>();

    @Getter
    private final int numAttributes;

    public FDSet(int numAttributes) {
        this.numAttributes = numAttributes;
    }

    public boolean add(BitSet equalAttrs) {
        int card = equalAttrs.cardinality();

        while (fdLevels.size() <= card){
            fdLevels.add(new HashSet<>());
        }

        return fdLevels.get(card).add((BitSet) equalAttrs.clone());
    }

    public boolean contains(BitSet equalAttrs) {
        int card = equalAttrs.cardinality();

        if (card >= fdLevels.size()){
            return false;
        }

        return fdLevels.get(card).contains(equalAttrs);
    }
}