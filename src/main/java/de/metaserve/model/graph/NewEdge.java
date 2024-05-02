package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metaserve.model.result.ResultSet;

import java.util.*;

public class NewEdge extends Edge{

    public void add(Collection<ColumnCombination> ccs){

    }

    public void add(Collection<ColumnCombination> left, Collection<ColumnCombination> right){

    }

    public List<String> getResultList(Node node){
        return null;
    }

    public ResultSet getResultTable() {
        ResultSet set = new ResultSet(Arrays.asList(left.name, right.name));
        for (Map.Entry<ColumnCombination, List<ColumnCombination>> entry: allNewEdges.entrySet()) {
            for (ColumnCombination cc : entry.getValue()) {
                set.add(Arrays.asList(entry.getKey().toString(), cc.toString()));
            }
        }
        return set;
    }

    public void update(Node node, Set<ColumnCombination> sccs) {
    }
}
