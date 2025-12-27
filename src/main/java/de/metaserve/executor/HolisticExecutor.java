package de.metaserve.executor;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.strategy.HolisticProfileStrategy;
import de.metaserve.executor.strategy.Strategy;
import de.metaserve.parser.query.Query;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.result.ResultSet;

import java.util.ArrayList;
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

        if(!configuration.getOutputType().equals(ExecutorConfiguration.Output.DEFAULT)){
            //Console.get().print(results);
        }

        query.getMetaData().update(QueryEngine.QueryState.QUERY_RESULT);

        ResultSet resultSet = new ResultSet(new ArrayList<>(graph.getNodes().keySet()));
        List<ResultSet> resultSets = new ArrayList<>();
        resultSets.add(resultSet);
        return resultSets;
    }
}
