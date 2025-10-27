package de.metaserve.parser.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UCC extends Dependency{
    public static final String NAME = "UCC";
    public List<String> ccFunction;
    public String id;
    public boolean not = false;


    public UCC(List<String> ccs, String id, boolean not){
        this.ccFunction = ccs;
        this.id = id;
        this.not = not;
    }

    public UCC(List<String> ccs, String id){
        this(ccs, id, false);
    }

    public String[] getSearchSpace(){
        return ccFunction.toArray(new String[ccFunction.size()]);
    }
    public List<String> parse(List<List<String>> columns){
        List<String> result = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            if(new HashSet<>(columns.get(i)).size() == columns.get(i).size()){
                result.add(ccFunction.get(i));
            }
        }
        return result;
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "UCC("+ id + ")";
    }
}
