package de.metaserve.executor.strategy;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.parser.query.QueryMetadata;
import de.metathesis.Instructor;
import de.metathesis.Preprocessor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class HolisticProfileStrategy implements Strategy{

    private final Graph graph;
    private final QueryMetadata metadata;
    private Map<String, List<String>> relationMap;
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
            List<Edge> orderedEdges = graph.getSetMembershipMap().entrySet().stream()
                            .sorted(Comparator.comparingInt(e -> e.getValue().priority()))
                            .map(Map.Entry::getKey)
                            .toList();

            /* this.relationMap = Map.of(
                    "X", Arrays.asList("R1", "R3", "R5"),
                    "Y", Arrays.asList("R3", "R4"),
                    "Z", Arrays.asList("R1", "R3", "R2")
            );*/

            Map<String, int[]> relationIndexMap = preprocessor.initializeSearchSpace(relationMap);

            instructor.runExecution(orderedEdges, relationIndexMap);
            //instructor.runPipeline(relationIndexMap);
            //instructor.runUCCOnly(relationIndexMap);
            metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
        } catch (AlgorithmConfigurationException |InputGenerationException e) {
            throw new RuntimeException(e);
        } finally {
            instructor.shutdownAndAwaitTermination();
        }
    }


    public static void print(Object o) {
        System.out.println(format(o));
    }

    private static String format(Object o) {
        if (o == null) return "null";

        if (o instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (var e : m.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(format(e.getKey()))
                        .append(": ")
                        .append(format(e.getValue()));
            }
            return sb.append("}").toString();
        }

        if (o instanceof int[] a) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(a[i]);
            }
            return sb.append("]").toString();
        }

        if (o instanceof Object[] a) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(format(a[i]));
            }
            return sb.append("]").toString();
        }

        if (o instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : it) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(format(x));
            }
            return sb.append("]").toString();
        }

        return o.toString();
    }

}
