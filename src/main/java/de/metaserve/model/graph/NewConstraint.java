package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;

import java.util.Set;

public abstract class NewConstraint extends NewEdge{

    public abstract Set<ColumnCombination> apply(Set<ColumnCombination> cc);
}
