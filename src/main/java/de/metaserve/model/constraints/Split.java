package de.metaserve.model.constraints;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Split implements PostCondition{
    @Getter
    String x;
    @Getter
    String y;
    @Override
    public String getName() {
        return "Split";
    }
}
