package de.metaserve.executor.min.constraints;

import java.util.Objects;

public class Interval {

    private int min;
    private int max;

    public Interval(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static Interval of(int min, int max) {
        return new Interval(min, max);
    }

    public static Interval singletion(int value) {
        return new Interval(value, value);
    }

    public static Interval atLeast(int min){
        return new Interval(min, Integer.MAX_VALUE);
    }

    public static Interval atMost(int max){
        return new Interval(1, max);
    }

    public static Interval combine(Interval current, Interval update){
        return merge(current, update);
    }

    public static Interval merge(Interval current, Interval update){
        if(current == null)
            return update;
        if (update == null)
            return current;
        return intersect(current, update);
    }

    public static Interval intersect(Interval current, Interval update){
        int lower = Math.max(current.min, update.min);
        int upper = Math.min(current.max, update.max);
        if (lower > upper)
            return null;
        return new Interval(lower, upper);
    }

    public boolean isSingletonOne(){
        return min == 1 && max == 1;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval interval = (Interval) o;
        return min == interval.min && max == interval.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return "Interval{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
