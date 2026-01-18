package de.metaserve.executor;

import de.metaserve.engine.QueryEngine;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.edge.FDEdge;
import de.metaserve.executor.min.graph.edge.INDEdge;
import de.metaserve.executor.min.graph.edge.UCCEdge;
import de.metaserve.executor.strategy.HolisticProfileStrategy;
import de.metaserve.executor.strategy.Strategy;
import de.metaserve.parser.graph.*;
import de.metaserve.parser.query.Query;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.result.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        List<ResultSet> results = graphToTuples(query.getConditions(), graph);
        applySelection(results, query.getSelections());
        applyRowFilter(results, query.getCCs(), query.getNumberOfTables());

        query.getMetaData().update(QueryEngine.QueryState.QUERY_RESULT);
        return results;
    }

    private List<ResultSet> graphToTuples(List<Condition> conditions, Graph graph) {
        List<ResultSet> list = new ArrayList<>();

        // Creating denormalized Table unless coupled only through contains or coalesce graph.getEdgesInBFS()
        for (List<Edge> cluster : graph.getClustersInBFS()){
            ResultSet resultSet = new ResultSet(new ArrayList<>(graph.getNodes().keySet()));
            for (Edge edge : cluster){
                if (edge instanceof INDEdge){
                    resultSet.addIND((INDEdge) edge);
                } else if (edge instanceof FDEdge){
                    resultSet.addMVFD((FDEdge) edge);
                } else if(edge instanceof UCCEdge && resultSet.getRows2().isEmpty()){
                    //System.out.println("Single UCC in cluster!");
                    resultSet.addUCC((UCCEdge) edge);
                }
            }
            list.add(resultSet);
        }

        if (list.size() > 1){
            //System.out.println("Clusters: " + list.size());
            ResultSet baseSet = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                for (Condition condition : conditions){
                    if (!(condition instanceof AbstractMerger)) continue;
                    if (((Mergeable) condition).applies(baseSet.getActiveColumns(), list.get(i).getActiveColumns())){
                        ((Mergeable) condition).merge(baseSet,list.get(i));
                        break;
                    }
                }
            }
            list.clear();
            list.add(baseSet);
        }

        // Applying post conditions and negation filter
        for (Condition condition : conditions){
            if (condition instanceof Split){
                Split split = (Split) condition;
                list.get(0).split(split.getX(),split.getY());
            } else if (condition instanceof Contains) {
                Contains contains = (Contains) condition;
                list.get(0).contains(contains.getX(),contains.getY());
            } else if (condition instanceof Coalesce) {
                Coalesce coalesce = (Coalesce) condition;
                list.get(0).coalesce(coalesce.getX(),coalesce.getY());
            } else if(condition instanceof UCC && ((UCC) condition).not){
                //System.out.println("Negation!");
                list.get(0).negativeUCC(((UCC) condition).id, ((UCC) condition).getSearchSpace());
            } /*else if(condition instanceof IND){
                list.get(0).cardinality(((IND) condition).leftName, ((IND) condition).rightName, "<");
            }
            */
        }

        return list;
    }

    private void applySelection(List<ResultSet> results, List<String> selections) {
        if (selections.contains("*"))
            return;
        for (ResultSet result : results){
            result.selectColumnNames(selections);
        }
    }

    private void applyRowFilter(List<ResultSet> results, Map<String, List<String>> columnNameToTableName, int numberOfTables) {
        if (results == null || results.isEmpty() || columnNameToTableName == null || columnNameToTableName.isEmpty()) {
            return;
        }
        for (ResultSet result : results) {
            if (result != null) {
                result.selectRowsByTableConstraints(columnNameToTableName, numberOfTables);
            }
        }
    }
}
