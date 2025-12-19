package de.metaserve.executor.strategy;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.parser.query.QueryMetadata;

public class HolisticProfileStrategy implements Strategy{

    private final Graph graph;
    private final QueryMetadata metadata;

    public HolisticProfileStrategy(Graph graph, QueryMetadata metadata){
        this.graph = graph;
        this.metadata = metadata;
    }

    @Override
    public void apply() {
        System.out.println("Holistic Profile Strategy Selected!");

        metadata.update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);

        //TODO: My Code is here.

        metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
    }
}
