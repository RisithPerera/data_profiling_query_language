package de.metaserve.executor;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.strategy.Strategy;
import de.metaserve.model.constraints.Condition;
import de.metaserve.model.dpal.Graph;
import de.metaserve.model.query.Query;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.ExecutorConfiguration;


import java.util.*;
import java.util.function.Function;

public class DPALExecutor implements Executor {

    private final ExecutorConfiguration configuration;

    public DPALExecutor(ExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<ResultSet> executeQuery(Query query) {
        Graph graph = Graph.fromConditions(query.getConditions());
        graph.computeSetMembership();

        Strategy.apply(graph, query.getMetaData());

        applyFilters(graph, query.getConditions());
        List<ResultSet> results = graphToTuples(graph);
        applySelection(results);
        applyAggregation(results);

        if(!configuration.getOutputType().equals(ExecutorConfiguration.Output.DEFAULT)){
            //Console.get().print(results);
        }
        query.getMetaData().update(QueryEngine.QueryState.QUERY_RESULT);
        return results;
    }

    private List<ResultSet> graphToTuples(Graph graph) {
        return null;
    }

    private void applyAggregation(List<ResultSet> results) {
    }

    private void applySelection(List<ResultSet> results) {

    }

    private void applyFilters(de.metaserve.model.dpal.Graph graph, List<Condition> conditions) {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void close() {

    }
}
