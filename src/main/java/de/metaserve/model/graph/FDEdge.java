package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metaserve.model.result.ResultSet;

import java.util.*;
import java.util.stream.Collectors;

public class FDEdge extends NewEdge {
    boolean negate = false;

    public FDEdge(boolean negate) {
        super();
        this.type = "FD";
        this.negate = negate;
    }

    @Override
    public void update(Node node, Set<ColumnCombination> sccs) {
        //updateNew();
        if(node.equals(left)){
            allNewEdges.keySet().removeIf(e -> !sccs.contains(e));
        } else {
            List<ColumnCombination> removeKey = new ArrayList<>();
            for (Map.Entry<ColumnCombination, List<ColumnCombination>> entry : allNewEdges.entrySet()) {
                List<ColumnCombination> removeValue = new ArrayList<>();
                for (ColumnCombination cc : entry.getValue()) {
                    if (!sccs.contains(cc))
                        removeValue.add(cc);
                }
                for (ColumnCombination cc : removeValue) {
                    allNewEdges.get(entry.getKey()).remove(cc);
                    if (allNewEdges.get(entry.getKey()).isEmpty())
                        removeKey.add(entry.getKey());
                }
            }
            for (ColumnCombination cc : removeKey) {
                allNewEdges.remove(cc);
            }
        }
    }

    @Override
    public List<String> getResultList(Node node) {
        List<String> resultList = new ArrayList<>();
        boolean left = node.equals(this.left);
        for (Map.Entry<ColumnCombination, List<ColumnCombination>> entry: allNewEdges.entrySet()) {
            int frequency = entry.getValue().size();
            for (ColumnCombination cc : entry.getValue()) {
                if(left)
                    resultList.add(entry.getKey().toString());
                else
                    resultList.add(cc.toString());
            }
        }
        return resultList;
    }

}
