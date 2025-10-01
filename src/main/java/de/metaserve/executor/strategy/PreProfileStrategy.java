package de.metaserve.executor.strategy;

import de.metanome.Metanome;
import de.metanome.algorithm_integration.results.Result;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.dpal.*;
import de.metaserve.model.query.QueryMetadata;
import de.metaserve.util.common.Quadruple;

import java.util.*;

public class PreProfileStrategy implements Strategy{

    Graph graph;
    QueryMetadata metadata;
    public PreProfileStrategy(Graph graph, QueryMetadata metadata){
        this.graph = graph;
        this.metadata = metadata;
    }

    @Override
    public void apply(Metanome metanome) {

        for (Edge edge : graph.getEdges()) {
            executeEdgeQuery(metanome, edge);
        }

        metadata.update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);
        Collection<Edge> minimal = graph.getMinimalEdges();
        List<ResultNode> resultNodes = new ArrayList<>();
        if (minimal.isEmpty()){
            System.out.println("No minimal");//@TODO
        }
        for (Edge min : minimal){
            min.mark();
            List<ResultNode> neighbors = graph.getResultNodes(min);
            if(neighbors.isEmpty())
                continue;
            Set<String> visited = new HashSet<>();
            Queue<ResultNode> queue = new LinkedList<>(neighbors);

            while(!queue.isEmpty()){
                ResultNode node = queue.poll();
                resultNodes.add(node);
                if(node.size() <= 1) continue;
                //BFS
                node.compute();
                for(String nodeName : node.getNextNodes()){
                    if(visited.contains(nodeName)) continue;
                    visited.add(nodeName);
                    ResultNode newNode = graph.getResultNode(nodeName);
                    newNode.minEdgeConnectedTo(node.name());
                    queue.add(newNode);
                }
            }
        }
        metadata.update(QueryEngine.QueryState.COMPUTED_MIN);


    }


    public void apply2(Metanome metanome) {

        for (Edge edge : graph.getEdges()) {
            executeEdgeQuery(metanome, edge);
        }
        metadata.update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);
        Collection<Edge> minimal = graph.getMinimalEdges();
        List<ResultNode> resultNodes = new ArrayList<>();
        if (minimal.isEmpty())
            throw new RuntimeException("No Minimal Found!");
        List<ResultNode> neighbors = graph.getResultNodes(minimal.stream().findFirst().get());

        boolean changed = true;
        while (changed) {
            changed = false;
            Set<String> visited = new HashSet<>();
            Queue<ResultNode> queue = new LinkedList<>(neighbors);
            while(!queue.isEmpty()){
                ResultNode node = queue.poll();
                resultNodes.add(node);
                if(node.size() <= 1) continue;
                //BFS
                boolean changes = node.compute();
                if (changes)
                    changed = true;
                for(String nodeName : node.getNextNodes()){
                    if(visited.contains(nodeName)) continue;
                    visited.add(nodeName);
                    queue.add(graph.getResultNode(nodeName));
                }
            }
        }
        metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
    }

    public boolean agreeSet(ResultsContainer main, Boolean mainSide, ResultsContainer right, Boolean rightSide){
        boolean backTrace = false;
        List<Quadruple<ResultWrapper, Boolean, ResultWrapper, Boolean>> counterOffer = new ArrayList<>();
        for(ResultWrapper result : main.getResults()){
            if(right.isValid(result, mainSide, rightSide)) continue;
            if(right.isSpezable(result, mainSide, rightSide)) continue;
            if(right.isSpezableWithChange(result, mainSide, rightSide)){
                right.spez(result, mainSide, rightSide);//gibt schon was mit result in kleiner splitte in zwei results
                continue;
            }
            ResultWrapper counter = right.getCounter(result, mainSide, rightSide);
            if(counter != null)
                counterOffer.add(new Quadruple<>(counter, rightSide, result, mainSide));

        }
        for (Quadruple<ResultWrapper, Boolean, ResultWrapper, Boolean> counter: counterOffer){
            boolean accept = main.reciveCounter(counter.first(), counter.second(), counter.third(), counter.fourth());
            right.endCounter(accept, counter.first());
            if(accept)
                backTrace = true;
        }
        return backTrace;
    }

    private void executeEdgeQuery(Metanome metanome, Edge edge) {
        String type = getEdgeType(edge);
        List<Result> result = null;

        switch (type) {
            case "IND":
                result = metanome.executeIND(edge.getSearchSpace());
                break;
            case "FD":
                result = metanome.executeFD(edge.getSearchSpace());
                break;
            case "UCC":
                result = metanome.executeUCC(edge.getSearchSpace());
                break;
            default:
                throw new RuntimeException("Found: " + type + " which is an unkown edge type!");
        }

        if (result != null) {
            metadata.addStat(type, result.size());
        }
        edge.setResults(result);
    }

    private String getEdgeType(Edge edge) {
        if (edge instanceof INDEdge) {
            return "IND";
        } else if (edge instanceof FDEdge) {
            return "FD";
        } else if (edge instanceof UCCEdge) {
            return "UCC";
        }
        return null;
    }

}
