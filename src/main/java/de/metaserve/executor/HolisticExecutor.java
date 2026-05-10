package de.metaserve.executor;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.strategy.HolisticProfileStrategy;
import de.metaserve.executor.strategy.Strategy;
import de.metaserve.parser.query.Query;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.result.ResultSet;

import java.util.List;

public class HolisticExecutor implements Executor {

    private final ExecutorConfiguration configuration;

    public HolisticExecutor(ExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<ResultSet> executeQuery(Query query) {
        Graph graph = Graph.fromConditions(query.getConditions());
        graph.computeSetMembership();

        Strategy strategy = new HolisticProfileStrategy(graph, query.getMetaData(), query.getCCs());
        strategy.apply();

        query.getMetaData().update(QueryEngine.QueryState.QUERY_RESULT);
        return List.of();
    }
}
