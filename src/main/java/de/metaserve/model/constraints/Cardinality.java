package de.metaserve.model.constraints;

public class Cardinality implements PostCondition {
    String x;
    String y;
    String operation;

    public Cardinality(String x, String y, String operation){
        this.operation = operation;
        this.x = x;
        this.y = y;
    }

    @Override
    public String getName() {
        return "Cardinality";
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getOperation() {
        return operation;
    }
}
