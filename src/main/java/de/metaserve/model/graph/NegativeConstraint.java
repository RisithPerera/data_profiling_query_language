package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;

import java.util.Collection;
import java.util.Set;

public class NegativeConstraint extends NewConstraint{
    boolean min;
    Collection<ColumnCombination> ccs;

    public NegativeConstraint(boolean min){
        this(min, null);
    }

    public NegativeConstraint(boolean min, Collection<ColumnCombination> ccs){
        this.min = min;
        this.ccs = ccs;
    }

    public void addAll(Collection<ColumnCombination> ccs){
        this.ccs = ccs;
    }

    @Override
    public Set<ColumnCombination> apply(Set<ColumnCombination> ccs) {
        ccs.removeIf(columnCombination -> this.ccs.contains(columnCombination));
        return ccs;
    }
}
