package de.metaserve.executor.min.results;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.results.FunctionalDependency;
import de.metanome.algorithm_integration.results.InclusionDependency;
import de.metanome.algorithm_integration.results.Result;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metaserve.executor.min.graph.edge.Edge;

import javax.ws.rs.NotSupportedException;
import java.util.*;

public class ResultNode {
    String name;
    List<Edge> edges = new ArrayList<>();
    Edge minEdg;

    public ResultNode(String name) {
        this.name = name;
    }

    public void add(Edge edge) {
        if (edge.isMarked())
            minEdg = edge;
        else
            edges.add(edge);
    }

    public int size() {
        return edges.size() + 1;
    }

    public boolean compute() {
        if (minEdg == null) // @TODO Check for marked edge?
            throw new RuntimeException("Missing min Edge in ResultNode!");

        Set<Set<String>> min = getSide(minEdg); //Min results
        List<Set<Set<String>>> allEdges = getSides(); //All other edges results
        Set<Set<String>> tempMin = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Set<Set<String>> edge : allEdges) { //Check that the min results are fine on all edges
                for (Set<String> subEdgeMinResult : new ArrayList<>(min)) {
                    boolean stay = false;
                    for (Set<String> supEdgeResult : edge) {
                        if (supEdgeResult.containsAll(subEdgeMinResult)) {
                            stay = true;
                            if(supEdgeResult.size() != subEdgeMinResult.size()) {
                                //System.out.println("Minimize:" + supEdgeResult + " " + subEdgeMinResult); @TODO Min Edge ist kleiner als Edge
                            }
                            break;
                        } else if (subEdgeMinResult.containsAll(supEdgeResult)){
                            //System.out.println(); Min Edge müsste kleiner werden
                        }
                    }
                    if (stay)
                        tempMin.add(subEdgeMinResult);
                }
            }
        }

        for (Edge edge : edges){
            for (Result result : new ArrayList<>(edge.results)){
                boolean stay = false;
                for (Set<String> actual : tempMin){
                    if (equalsBetween(result, actual, edge.leftName.equals(name))){
                        stay = true;
                        break;
                    }
                }
                if (!stay) {
                    edge.results.remove(result);
                    //System.out.println("Remove: " + result);
                }
            }
            //System.out.println(edge.results);
        }
        //System.out.println(tempMin);
        return changed;
    }

    private boolean equalsBetween(Result result, Set<String> actual, boolean leftSide) {
        if (result instanceof InclusionDependency) {
            InclusionDependency indResult = (InclusionDependency) result;
            if (!leftSide)
                return permutationToSet(indResult.getReferenced()).containsAll(actual);
            else
                return permutationToSet(indResult.getDependant()).containsAll(actual);
        } else if (result instanceof FunctionalDependency) {
            FunctionalDependency fdResult = (FunctionalDependency) result;
            if (leftSide)
                return combinationToSet(fdResult.getDeterminant()).containsAll(actual);
            else
                return identifierToSet(fdResult.getDependant()).containsAll(actual);
        } else if (result instanceof UniqueColumnCombination) {
            UniqueColumnCombination uccResult = (UniqueColumnCombination) result;
            return combinationToSet(uccResult.getColumnCombination()).containsAll(actual);
        } else {
            throw new NotSupportedException();
        }
    }

    private List<Set<Set<String>>> getSides() {
        List<Set<Set<String>>> result = new ArrayList<>();
        for (Edge edge : edges) {
            result.add(getSide(edge));
        }
        return result;
    }

    private Set<Set<String>> getSide(Edge edge) {
        return getSide(edge.results, edge.leftName.equals(name));
    }

    private Set<Set<String>> getSide(List<Result> results, boolean leftSide) {
        Set<Set<String>> refinedResults = new HashSet<>();
        for (Result result : results) {
            if (result instanceof InclusionDependency) {
                InclusionDependency indResult = (InclusionDependency) result;
                if (!leftSide)
                    refinedResults.add(permutationToSet(indResult.getReferenced()));
                else
                    refinedResults.add(permutationToSet(indResult.getDependant()));
            } else if (result instanceof FunctionalDependency) {
                FunctionalDependency fdResult = (FunctionalDependency) result;
                if (leftSide)
                    refinedResults.add(combinationToSet(fdResult.getDeterminant()));
                else
                    refinedResults.add(identifierToSet(fdResult.getDependant()));
            } else if (result instanceof UniqueColumnCombination) {
                UniqueColumnCombination uccResult = (UniqueColumnCombination) result;
                refinedResults.add(combinationToSet(uccResult.getColumnCombination()));
            } else {
                throw new NotSupportedException();
            }
        }
        return refinedResults;
    }

    private Set<String> identifierToSet(ColumnIdentifier id) {
        return new HashSet<>(Collections.singleton(id.toString()));
    }

    private Set<String> combinationToSet(ColumnCombination combination) {
        Set<String> set = new HashSet<>();
        for (ColumnIdentifier id : combination.getColumnIdentifiers())
            set.add(id.toString());
        return set;
    }

    private Set<String> permutationToSet(ColumnPermutation permutation) {
        Set<String> set = new HashSet<>();
        for (ColumnIdentifier id : permutation.getColumnIdentifiers())
            set.add(id.toString());
        return set;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public String name() {
        return name;
    }

    public List<String> getNextNodes() {
        List<String> nodeNames = new ArrayList<>();
        List<Edge> edgeList = new ArrayList<>(edges);
        if (minEdg != null)
            edgeList.add(minEdg);
        for (Edge edge : edgeList) {
            if (!edge.leftName.equals(name) && !nodeNames.contains(edge.leftName))
                nodeNames.add(edge.leftName);
            else if (!edge.rightName.equals(name) && !nodeNames.contains(edge.rightName))
                nodeNames.add(edge.rightName);
        }
        return nodeNames;
    }

    public void minEdgeConnectedTo(String name) {
        for (Edge edge : edges){
            if (edge.leftName.equals(name) ||edge.rightName.equals(name)){
                minEdg = edge;
                break;
            }
        }
        if (minEdg == null)
            throw new RuntimeException("No min Edge!");
    }
}
