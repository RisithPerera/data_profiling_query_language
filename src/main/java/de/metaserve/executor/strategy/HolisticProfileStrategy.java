package de.metaserve.executor.strategy;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.parser.query.QueryMetadata;
import de.metathesis.Instructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HolisticProfileStrategy implements Strategy{

    private final Graph graph;
    private final QueryMetadata metadata;
    private final Map<String, List<String>> queryCCs;

    public HolisticProfileStrategy(Graph graph, QueryMetadata metadata, Map<String, List<String>> queryCCs) {
        this.graph = graph;
        this.metadata = metadata;
        this.queryCCs = queryCCs;
    }

    @Override
    public void apply() {
        System.out.println("Holistic Profile Strategy Selected!");

        metadata.update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);

        System.out.println(queryCCs);
        for (Edge edge : graph.getSetMembershipMap().keySet()) {
            System.out.println(edge + " " + graph.getSetMembershipMap().get(edge));
        }

        //This is just calling current instructor. But before this inside this class or inside instructor need to first find the strategy.
        // Then based on it run pipeline.
        System.out.println("Pool Count: " + Runtime.getRuntime().availableProcessors());

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Instructor instructor = Instructor.getInstance(pool);

        instructor.runPipeline(3);


        metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
    }
}
