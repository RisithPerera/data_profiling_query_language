package de.metaserve.model.constraints;

import java.util.*;

public class FD extends Dependency{
    public static final String NAME = "FD";
    public List<String> left;
    public List<String> right;
    public final String leftName;
    public final String rightName;

    public FD(List<String> left, List<String> right, String leftName, String rightName){
        this.left = left;
        this.right = right;
        this.leftName = leftName;
        this.rightName = rightName;
    }

    public List<Pair<String, String>> parse(List<List<String>> left, List<List<String>> right){
        List<Pair<String, String>> results = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            for (int j = 0; j < right.size(); j++) {
                if(isFD(left.get(i), right.get(j))){
                    results.add(new Pair<>(this.left.get(i), this.right.get(j)));
                }
            }
        }
        return results;
    }

    private boolean isFD(List<String> left, List<String> right) {
        if(left.size() != right.size())
            return false;
        Map<String, String> fd = new HashMap<>();
        for (int i = 0; i < left.size(); i++) {
            String leftValue = left.get(i);
            String rightValue = right.get(i);
            if(!fd.containsKey(leftValue))
                fd.put(leftValue, rightValue);
            else if(!fd.get(leftValue).equals(rightValue))
                return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
