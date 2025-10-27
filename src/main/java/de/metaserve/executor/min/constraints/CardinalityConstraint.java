package de.metaserve.executor.min.constraints;

import de.metaserve.parser.graph.Condition;

public class CardinalityConstraint implements Condition {

    public static final String NAME = "CARD";
    String target;
    Interval interval;

    public CardinalityConstraint(String target, Interval interval) {
        this.target = target;
        this.interval = interval;
    }

    @Override
    public String getName() {
        return "CARD";
    }

    public String getTarget() {
        return target;
    }

    public Interval getInterval() {
        return interval;
    }
}
