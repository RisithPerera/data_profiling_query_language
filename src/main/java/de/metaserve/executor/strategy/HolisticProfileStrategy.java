package de.metaserve.executor.strategy;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.parser.query.QueryMetadata;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.exceptions.AlgorithmConfigurationException;
import de.metaserve.util.exceptions.InputGenerationException;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metathesis.Instructor;
import de.metathesis.ResultFormatter;
import de.metathesis.profilers.results.Result;
import de.metathesis.structures.ResultTable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HolisticProfileStrategy implements Strategy{

    private final Graph graph;
    private final QueryMetadata metadata;
    private final Map<String, List<String>> relationMap;
    private final ExecutorService executor;
    private final ResultFormatter resultFormatter;

    public HolisticProfileStrategy(Graph graph, QueryMetadata metadata, Map<String, List<String>> relationMap) {
        this.graph = graph;
        this.metadata = metadata;
        this.relationMap = relationMap;
        this.resultFormatter = ResultFormatter.getInstance();

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
            System.out.println(this.graph.getSetMembershipMap());
            Map<Edge, Result<?>> results = instructor.runExecutionMethod2(this.graph.getSetMembershipMap(), this.relationMap);
            List<ResultTable> schema = this.resultFormatter.createResultSchema(results);

            InputConfiguration inputConfig = EngineConfigurationSingleton.get().getInputConfig();

            if(inputConfig.getNORMALIZE_RESULTS()){
                for(ResultTable table : schema){
                    //table.toFile("b1_holl.txt");
                    System.out.println(table);
                }

                System.out.println(schema.stream()
                        .map(table -> table.getColumnNames() + "=" + table.size())
                        .collect(Collectors.joining(", ")));
            }else{
                if (schema.isEmpty()) return;

                ResultTable joined = schema.get(0);
                for (int i = 1; i < schema.size(); i++) {
                    joined = this.resultFormatter.join(joined, schema.get(i));
                }

                //joined.toFile("b1_holl.txt");
                System.out.println(joined);
                System.out.println("Joined Result — Size: " + joined.size());

                System.out.println(schema.stream()
                        .map(table -> table.getColumnNames() + "=" + table.size())
                        .collect(Collectors.joining(", ")));
            }

        } catch (AlgorithmConfigurationException | InputGenerationException e) {
            throw new RuntimeException(e);
        } finally {
            metadata.update(QueryEngine.QueryState.COMPUTED_MIN);
            instructor.shutdownAndAwaitTermination();
        }
    }
}
