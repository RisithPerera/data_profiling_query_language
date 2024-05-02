package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;

public class Constraint {
    public String name() {
        return "";
    }

    enum CCStatus{
        VALID,
        INVALID,
        GENERALIZE,
        SPECIFY
    }
    public CCStatus test(ColumnCombination cc) {
        return null;
    }
}
