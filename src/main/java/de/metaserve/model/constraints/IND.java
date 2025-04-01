package de.metaserve.model.constraints;

import de.metaserve.util.common.Pair;

import java.util.ArrayList;
import java.util.List;

public class IND extends Dependency{
    public static final String NAME = "IND";
    public List<String> left;
    public List<String> right;
    public String leftName;
    public String rightName;

    public IND(List<String> left, List<String> right, String leftName, String rightName){
        this.left = left;
        this.right = right;
        this.leftName = leftName;
        this.rightName = rightName;
    }

    public List<Pair<String, String>> parse(List<List<String>> left, List<List<String>> right){
        List<Pair<String, String>> results = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            for (int j = 0; j < right.size(); j++) {
                if(isIND(left.get(i), right.get(j))){
                    results.add(new Pair<>(this.left.get(i), this.right.get(j)));
                }
            }
        }
        return results;
    }

    private boolean isIND(List<String> left, List<String> right) {
        return left.containsAll(right);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "IND("+ leftName + " < " + rightName + ")";
    }
}
