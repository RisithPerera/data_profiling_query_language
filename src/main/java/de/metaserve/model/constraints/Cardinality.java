package de.metaserve.model.constraints;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Cardinality implements PostCondition{
    @Getter
    String x;
    @Getter
    String y;
    @Getter
    String operation;

    @Override
    public String getName() {
        return "Cardinality";
    }
}
