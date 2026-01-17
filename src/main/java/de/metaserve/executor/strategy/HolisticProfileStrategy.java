package de.metaserve.executor.strategy;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.parser.query.QueryMetadata;
import de.metathesis.Instructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HolisticProfileStrategy implements Strategy{

    private final Graph graph;
    private final QueryMetadata metadata;
    private final Map<String, List<String>> relationMap;
    private final ExecutorService executor;

    public HolisticProfileStrategy(Graph graph, QueryMetadata metadata, Map<String, List<String>> relationMap) {
        this.graph = graph;
        this.metadata = metadata;
        this.relationMap = relationMap;

        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        System.out.println("Fixed Thread Pool Count: " + threadPoolSize);

        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public void apply() {
        System.out.println("Holistic Profile Strategy Applied!");

        metadata.update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);

        Instructor instructor = new Instructor(this.executor);

        try {
            instructor.runExecution(this.graph.getSetMembershipMap(), this.relationMap);

        } catch (AlgorithmConfigurationException |InputGenerationException e) {
            throw new RuntimeException(e);
        } finally {
            metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
            instructor.shutdownAndAwaitTermination();
        }
    }
}
