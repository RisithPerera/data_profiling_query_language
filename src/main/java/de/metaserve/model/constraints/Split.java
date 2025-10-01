package de.metaserve.model.constraints;


public class Split implements PostCondition{

    String x;

    String y;

    public Split(String x, String y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String getName() {
        return "Split";
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }
}
