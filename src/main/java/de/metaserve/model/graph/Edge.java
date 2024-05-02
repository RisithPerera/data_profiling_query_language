package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;

import java.util.*;
import java.util.stream.Collectors;

public class Edge {
    String type;
    Node left;
    Node right;
    Map<ColumnCombination, List<ColumnCombination>> allEdges = new HashMap<>();
    Map<ColumnCombination, List<ColumnCombination>> allNewEdges = new HashMap<>();
    List<CCEdge> miniEdges = new ArrayList<>();

    public Edge() {
    }

    public Edge(String type, Node leftNode, Node rightNode) {
        this.type = type;
        this.left = leftNode;
        this.right = rightNode;
    }

    void updateNew(){
        if(allNewEdges.isEmpty()){
            allNewEdges = new HashMap<>(allEdges);
        }
    }

    public static NewEdge getSubType(boolean negate, String type) {
        if(negate){
            switch (type) {
                case "UCC":
                case "FD":
                    return new NegativeConstraint(true);
                case "IND":
                    return new NegativeConstraint(false);
            }
        } else {
            switch (type) {
                case "UCC":
                    return new UCCEdge(negate);
                case "FD":
                    return new FDEdge(negate);
                case "IND":
                    return new INDEdge(negate);
            }
        }
        return null;
    }

    public void add(ColumnCombination leftCC, ColumnCombination rightCC) {
        if(!allEdges.containsKey(leftCC))
            allEdges.put(leftCC, new ArrayList<>());
        allEdges.get(leftCC).add(rightCC);
        miniEdges.add(new CCEdge(leftCC, rightCC));
    }

    public List<String> getResults(Node node, List<ColumnCombination> result, Collection<String> negative) {
        List<String> resultList = new ArrayList<>();
        if(node.equals(left)){
            List<ColumnCombination> removeKey = new ArrayList<>();
            for (ColumnCombination cc1: allEdges.keySet()) {
                List<ColumnIdentifier> removeValue = new ArrayList<>();
                if((result.isEmpty() || result.contains(cc1)) && !negative.contains(cc1.toString())) {
                    for (ColumnCombination ref : allEdges.get(cc1)) {
                        resultList.add(cc1.toString());
                    }
                } else
                    removeKey.add(cc1);
            }
            for (ColumnCombination cc: removeKey) {
                allEdges.remove(cc);
            }
        } else {
            List<ColumnCombination> removeKey = new ArrayList<>();
            for (Map.Entry<ColumnCombination, List<ColumnCombination>> entry: allEdges.entrySet()) {
                List<ColumnCombination> removeValue = new ArrayList<>();
                for (ColumnCombination cc1: entry.getValue()) {
                    if(result.isEmpty())
                        resultList.add(cc1.toString());
                    else if(result.contains(cc1))
                        resultList.add(cc1.toString());
                    else
                        removeValue.add(cc1);
                }
                for (ColumnCombination cc2: removeValue) {
                    allEdges.get(entry.getKey()).remove(cc2);
                    if(allEdges.get(entry.getKey()).isEmpty())
                        removeKey.add(entry.getKey());
                }
            }
            for (ColumnCombination cc : removeKey) {
                allEdges.remove(cc);
            }
        }

        return resultList;
    }

    public boolean isMaxRequired(){
        return type.equals("IND") && left.result.isEmpty() && right.result.isEmpty();
    }

    public void maximize() {
        Set<ColumnCombination> toDelete = new HashSet<>();
        for (ColumnCombination cc1: allEdges.keySet()) {
            if(toDelete.contains(cc1))
                continue;
            for (ColumnCombination cc2: allEdges.keySet()) {
                if(cc1.equals(cc2))
                    continue;
                if(cc1.getColumnIdentifiers().containsAll(cc2.getColumnIdentifiers()))
                    toDelete.add(cc2);
            }
        }
        for (ColumnCombination cc : toDelete) {
            allEdges.remove(cc);
        }
    }

    public Set<ColumnCombination> getSide(boolean left) {
        Set<ColumnCombination> sideSet = new HashSet<>();
        if(left){
            sideSet.addAll(allEdges.keySet());
        } else {
            sideSet.addAll(allEdges.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        }
        return sideSet;
    }

    public Set<ColumnCombination> getSide(Node node) {
        Set<ColumnCombination> sideSet = new HashSet<>();
        if(node.equals(left)){
            sideSet.addAll(allEdges.keySet());
        } else {
            sideSet.addAll(allEdges.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        }
        return sideSet;
    }

    public Set<ColumnCombination> getNewSide(Node node) {
        Set<ColumnCombination> sideSet = new HashSet<>();
        if(node.equals(left)){
            sideSet.addAll(allNewEdges.keySet());
        } else {
            sideSet.addAll(allNewEdges.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        }
        return sideSet;
    }

    public Node getOtherSide(Node node) {
        if(node.equals(left))
            return right;
        return left;
    }
}
