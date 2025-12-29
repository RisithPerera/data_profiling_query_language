package de.metaserve.executor.strategy;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.parser.query.QueryMetadata;
import de.metathesis.Instructor;
import de.metathesis.Preprocessor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class HolisticProfileStrategy implements Strategy{

    private final Graph graph;
    private final QueryMetadata metadata;
    private final Map<String, List<String>> relationMap;
    private final Instructor instructor;
    private final Preprocessor preprocessor;

    public HolisticProfileStrategy(Graph graph, QueryMetadata metadata, Map<String, List<String>> relationMap) {
        this.graph = graph;
        this.metadata = metadata;
        this.relationMap = relationMap;

        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        System.out.println("Fixed Thread Pool Count: " + threadPoolSize);
        this.instructor = Instructor.getInstance(Executors.newFixedThreadPool(threadPoolSize));
        this.preprocessor = Preprocessor.getInstance();
    }

    @Override
    public void apply() {
        System.out.println("Holistic Profile Strategy Applied!");

        metadata.update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);
        
        try {
            Map<String, int[]> relationIndexMap = preprocessor.initializeSearchSpace(relationMap);

            instructor.runPipeline(3);

            metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
        } catch (AlgorithmConfigurationException |InputGenerationException e) {
            throw new RuntimeException(e);
        } finally {
            instructor.shutdownAndAwaitTermination();
        }
    }
}
