package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;

public class CCEdge {
    boolean valid = true;
    ColumnCombination leftCC;
    ColumnCombination rightCC;
    public CCEdge(ColumnCombination leftCC, ColumnCombination rightCC) {
        this.leftCC = leftCC;
        this.rightCC = rightCC;
    }
}
