package de.metaserve.util.common;

import de.metaserve.executor.min.graph.*;
import de.metaserve.executor.min.graph.edge.FDEdge;
import de.metaserve.executor.min.graph.edge.INDEdge;
import de.metaserve.executor.min.graph.edge.UCCEdge;

import java.util.*;

public class GraphGenerator {

    private Set<Graph> resultList = new HashSet<>();
    private int globalK;
    public Set<Graph> generateGraphs(int k) {
        return generateGraphs(k, false);
    }

    public Set<Graph> generateGraphs(int k, boolean save) {
        globalK = k+1;
        resultList = new HashSet<>();
        if(GraphUtil.hasGraphFile(k)){
            resultList = GraphUtil.readGraphs(k);
        } else {
            generateGraphs(k, null, null);
        }
        if(save) GraphUtil.saveGraphs(resultList, k);
        return resultList;
    }

    public Set<Graph> generateGraphs(int k, Graph currentGraph) {
        globalK = k+1;
        resultList = new HashSet<>();
        generateGraphs(k, currentGraph, k);
        return resultList;
    }

    public void generateGraphs(int k, Graph currentGraph, Integer realK) {
        if (currentGraph == null) {
            currentGraph = new Graph();
            currentGraph.addNode(getNodeNameK(k+1));
            realK = k;
        }
        /**
         else {
         String oldNodeName = "node_" + (realK + 1);
         if (!currentGraph.getNodes().containsKey(oldNodeName)){
         oldNodeName = "node_" + (realK + 2);
         }
         List<Edge> incomingEdges = currentGraph.getNodes().get(oldNodeName).getEdgesIncoming();
         List<Edge> outgoingEdges = currentGraph.getNodes().get(oldNodeName).getEdgesOutgoing();

         if (incomingEdges.isEmpty() && outgoingEdges.isEmpty())
         return;
         if (currentGraph.getNodes().size() > 1 && incomingEdges.size() == 1 && outgoingEdges.size() == 1 && currentGraph.getNodes().get(oldNodeName).getEdgesIncoming().get(0) instanceof UCCEdge) {
         return;
         }

         }**/

        if (resultContainsGraph(resultList, currentGraph)) {
            return;
        }

        if (k == 0) {
            resultList.add(currentGraph);
            return;
        }

        String newNodeName = getNodeName(currentGraph.getNodes().size());

        for (Node node : currentGraph.getNodes().values()) {
            if (hasEdge(currentGraph, node, node, "UCC")) {
                continue;
            }
            Graph selfLoopGraph = currentGraph.copy();
            selfLoopGraph.addEdge(new UCCEdge(node.getName())); //Self Edge
            generateGraphs(k - 1, selfLoopGraph, realK);
        }

        currentGraph.addNode(newNodeName);

        for (Node node1 : currentGraph.getNodes().values()) {
            for (Node node2 : currentGraph.getNodes().values()) {
                if (!node1.equals(node2)) {
                    if (hasEdge(currentGraph, node1, node2, "FD")) {
                        continue;
                    }
                    Graph directEdgeFGraph = currentGraph.copy();
                    directEdgeFGraph.addEdge(new FDEdge(node1.getName(), node2.getName()));
                    if (directEdgeFGraph.getNode(newNodeName).isEmpty()) {
                        directEdgeFGraph.removeNode(newNodeName);
                    }
                    generateGraphs(k - 1, directEdgeFGraph, realK - 1);
                }
            }
        }
        for (Node node1 : currentGraph.getNodes().values()) {
            for (Node node2 : currentGraph.getNodes().values()) {
                if (!node1.equals(node2)) {
                    if (hasEdge(currentGraph, node1, node2, "IND")) {
                        continue;
                    }
                    Graph directEdgeIGraph = currentGraph.copy();
                    directEdgeIGraph.addEdge(new INDEdge(node1.getName(), node2.getName()));
                    if (directEdgeIGraph.getNode(newNodeName).isEmpty()) {
                        directEdgeIGraph.removeNode(newNodeName);
                    }
                    generateGraphs(k - 1, directEdgeIGraph, realK - 1);
                }
            }
        }
    }

    HashMap<Set<String>, List<List<String>>> permutationMap = new HashMap<>();
    Set<Graph> allPermutations = new HashSet<>();
    private boolean resultContainsGraph(Set<Graph> resultList, Graph currentGraph) {
        if(resultList.contains(currentGraph))
            return true;
        List<List<String>> permutations;
        if(permutationMap.containsKey(currentGraph.getNodes().keySet())){
            permutations = permutationMap.get(currentGraph.getNodes().keySet());
        } else {
            permutations = getPermutations(new ArrayList<>(currentGraph.getNodes().keySet()));
            permutationMap.put(currentGraph.getNodes().keySet(), permutations);
        }
        Set<Graph> allPermutationsLocal = new HashSet<>();
        for (List<String> permutation : permutations){
            Graph newGraph = currentGraph.copyMap(permutation);
            if(resultList.contains(newGraph) || allPermutations.contains(newGraph)){
                allPermutations.addAll(allPermutationsLocal);
                return true;
            } else{
                allPermutationsLocal.add(newGraph);
            }
        }
        allPermutations.addAll(allPermutationsLocal);
        return false;
    }
    private String getNodeName(int i) {
        return Character.toString((char) ('A' + i));
    }
    private String getNodeNameK(int i) {
        int number = globalK - i;
        return Character.toString((char) ('A' + number));
    }
    private boolean hasEdge(Graph currentGraph, Node node, Node node1, String type) {
        if(type.equals("UCC")){
            return currentGraph.hasEdge(new UCCEdge(node.getName()));
        } else if (type.equals("FD")){
            return currentGraph.hasEdge(new FDEdge(node.getName(), node1.getName()));
        }else if (type.equals("IND")){
            return currentGraph.hasEdge(new INDEdge(node.getName(), node1.getName()));
        } else {
            throw new RuntimeException("Not allowed type!");
        }
    }

    public static List<List<String>> getPermutations(List<String> list) {
        List<List<String>> result = new ArrayList<>();
        generatePermutations(list, 0, result);
        return result;
    }

    private static void generatePermutations(List<String> list, int start, List<List<String>> result) {
        if (start >= list.size() - 1) {
            result.add(new ArrayList<>(list));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            Collections.swap(list, start, i);
            generatePermutations(list, start + 1, result);
            Collections.swap(list, start, i); // backtrack
        }
    }
}