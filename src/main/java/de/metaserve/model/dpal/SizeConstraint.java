package de.metaserve.model.dpal;

import de.metaserve.model.constraints.Condition;

public class SizeConstraint implements Condition {

    public static final String NAME = "SIZE";
    String target;
    Interval interval;

    public SizeConstraint(String target, Interval interval) {
        this.target = target;
        this.interval = interval;
    }

    @Override
    public String getName() {
        return "SIZE";
    }

    public String getTarget() {
        return target;
    }

    public Interval getInterval() {
        return interval;
    }
}
