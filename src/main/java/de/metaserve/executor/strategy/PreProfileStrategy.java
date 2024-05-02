package de.metaserve.executor.strategy;

import de.metanome.Metanome;
import de.metanome.algorithm_integration.results.Result;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.constraints.Pair;
import de.metaserve.model.dpal.*;
import de.metaserve.model.query.QueryMetadata;

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
        for (Edge min : minimal){
            Queue<Quadruple<Edge, Boolean, Edge, Boolean>> queue = new LinkedList<>();
            List<Triple<Boolean, Edge, Boolean>> neighbors = min.getNeighborsWithDir();
            if(neighbors.isEmpty())
                continue;
            for(Triple<Boolean, Edge, Boolean> triple : neighbors){
                queue.add(new Quadruple<>(min, triple.getFirst(), triple.getSecond(), triple.getThird()));
            }

            while(!queue.isEmpty()){
                Quadruple<Edge, Boolean, Edge, Boolean> quad = queue.poll();
                //BFS
                neighbors = quad.getFirst().getNeighborsWithDir();
                for(Triple<Boolean, Edge, Boolean> triple : neighbors){
                    queue.add(new Quadruple<>(min, triple.getFirst(), triple.getSecond(), triple.getThird()));
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
            boolean accept = main.reciveCounter(counter.getFirst(), counter.getSecond(), counter.getThird(), counter.getFourth());
            right.endCounter(accept, counter.getFirst());
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
