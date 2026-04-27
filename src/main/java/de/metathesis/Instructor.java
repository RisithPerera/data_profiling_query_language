package de.metathesis;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metathesis.profilers.ProfilerFactory;
import de.metathesis.structures.ExecutionNode;
import de.metathesis.structures.ResultTable;
import de.metathesis.utils.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class Instructor {
    private static final Logger log = LogManager.getLogger(Instructor.class);

    private final ExecutorService pool;
    private final Preprocessor preprocessor;
    private final ProfilerFactory profilerFactory;
    private final ResultFormatter resultFormatter;

    public Instructor(ExecutorService pool) {
        this.pool = pool;
        this.preprocessor = Preprocessor.getInstance();
        this.profilerFactory = new ProfilerFactory(pool);
        this.resultFormatter = ResultFormatter.getInstance();
    }

    public void runExecutionMethod1(Map<Edge, Graph.SetMembership> setMembershipMap, Map<String, List<String>> relationMap) throws InputGenerationException, AlgorithmConfigurationException {
        Map<String, int[]> relationIndexMap = this.preprocessor.initializeSearchSpace(relationMap);
        Map<String, int[]> relationSizesMap = this.preprocessor.getAttributeSizesOf(relationIndexMap);

        //Make an order list of dependencies based on set membership priority order
        List<Edge> orderedEdges = setMembershipMap.entrySet().stream()
                .sorted(Comparator
                    // 1. primary: enum priority (lower first)
                    .comparingInt((Map.Entry<Edge, Graph.SetMembership> e) -> e.getValue().priority())

                    // 2. secondary: dependency count (higher first)
                    .thenComparing(e -> e.getKey().degree(), Comparator.reverseOrder())
                )
                .map(Map.Entry::getKey)
                .toList();

        int globalMaxLevel = Utility.max(relationSizesMap.values());

        Map<String, ExecutionNode> executionGraph = new LinkedHashMap<>(); //All Graph Nodes

        for (int level = 0; level <= globalMaxLevel; level++) {
            // Maps last Execution Node (Edge) used by the variable
            Map<String, ExecutionNode> lastNodeByVariable = new HashMap<>();

            for (Edge edge : orderedEdges) {
                ExecutionNode node = new ExecutionNode(edge, level, profilerFactory);

                //Filter Relations based on the column size and level
                final int[] lhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get(edge.leftName), relationSizesMap.get(edge.leftName), level);
                final int[] rhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get(edge.rightName), relationSizesMap.get(edge.rightName), level);

                node.getRelationMap().put(edge.leftName, lhsRelationIndexes);
                node.getRelationMap().put(edge.rightName, rhsRelationIndexes);

                final boolean isLhsLocked = lastNodeByVariable.containsKey(edge.leftName);
                final boolean isRhsLocked = lastNodeByVariable.containsKey(edge.rightName);

                final ExecutionNode lhsPreviousNode = lastNodeByVariable.get(edge.leftName);
                final ExecutionNode rhsPreviousNode = lastNodeByVariable.get(edge.rightName);

                // Dependency: previous level of same edge
                if (!isRhsLocked && !isLhsLocked && executionGraph.containsKey(node.getPreviousNodeId())) {
                    node.getParents().put("#", executionGraph.get(node.getPreviousNodeId()));
                }

                // Dependency: last use of variables
                if (edge.leftName != null && isLhsLocked) {
                    node.getParents().put(edge.leftName, lhsPreviousNode);
                    lhsPreviousNode.getChildren().add(node);
                }

                if (edge.rightName != null && isRhsLocked) {
                    node.getParents().put(edge.rightName, rhsPreviousNode);
                    rhsPreviousNode.getChildren().add(node);
                }

                // Register node
                executionGraph.put(node.toString(), node);

                if (edge.leftName != null) {
                    lastNodeByVariable.put(edge.leftName, node);
                }

                if (edge.rightName != null) {
                    lastNodeByVariable.put(edge.rightName, node);
                }
            }
        }

        //printGraph(executionGraph);

        //Nodes Should be in order
        for (ExecutionNode node : executionGraph.values()) {
            node.execute();
        }

        log.info(Utility.buildLog("SCHEDULED", this.pool));

        List<ExecutionNode> leafNodes = executionGraph.values().stream()
                .filter(n -> n.getChildren().isEmpty())
                .toList();

        // Join leaves
        CompletableFuture.allOf(leafNodes.stream()
                .map(ExecutionNode::getFuture)
                .toArray(CompletableFuture[]::new)
        ).join();
        log.info(Utility.buildLog("FINISHED", this.pool));

        List<ResultTable> schema = this.resultFormatter.createResultSchema(executionGraph);

        if(schema.size() > 1) {
            ResultTable joinedTable = this.resultFormatter.join(schema.getFirst(), schema.getLast());
            System.out.println(joinedTable.size());
            System.out.println(joinedTable);
        }else{
            schema.getFirst().toFile("b1_holl.txt");
            //System.out.println(schema.getFirst());
        }

        System.out.println("---------------------------------------------------------------------");
        for(ResultTable table : schema){
            System.out.println("Table: " + table.getColumnNames() +" Size: "+ table.size());
        }
    }

    public void runExecutionMethod2(Map<Edge, Graph.SetMembership> setMembershipMap, Map<String, List<String>> relationMap) throws InputGenerationException, AlgorithmConfigurationException {
        Map<String, int[]> relationIndexMap = this.preprocessor.initializeSearchSpace(relationMap);
        Map<String, int[]> relationSizesMap = this.preprocessor.getAttributeSizesOf(relationIndexMap);

        //Make an order list of dependencies based on set membership priority order
        List<Edge> orderedEdges = getEdgeOrder(setMembershipMap);

        int globalMaxLevel = Utility.max(relationSizesMap.values());

        Map<String, ExecutionNode> executionGraph = new LinkedHashMap<>(); //All Graph Nodes
        //globalMaxLevel = 1;
        for (int level = 0; level <= globalMaxLevel; level++) {
            // Maps last Execution Node (Edge) used by the variable
            Map<String, ExecutionNode> firstNodeByVariable = new HashMap<>();

            for (Edge edge : orderedEdges) {
                ExecutionNode node = new ExecutionNode(edge, level, profilerFactory);

                //Filter Relations based on the column size and level
                final int[] lhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get(edge.leftName), relationSizesMap.get(edge.leftName), level);
                final int[] rhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get(edge.rightName), relationSizesMap.get(edge.rightName), level);

                node.getRelationMap().put(edge.leftName, lhsRelationIndexes);
                node.getRelationMap().put(edge.rightName, rhsRelationIndexes);

                final boolean isLhsLocked = firstNodeByVariable.containsKey(edge.leftName);
                final boolean isRhsLocked = firstNodeByVariable.containsKey(edge.rightName);

                final ExecutionNode lhsPreviousNode = firstNodeByVariable.get(edge.leftName);
                final ExecutionNode rhsPreviousNode = firstNodeByVariable.get(edge.rightName);

                // Dependency: previous level of same edge
                if (!isRhsLocked && !isLhsLocked && executionGraph.containsKey(node.getPreviousNodeId())) {
                    node.getParents().put("#", executionGraph.get(node.getPreviousNodeId()));
                }

                // Dependency: last use of variables
                if (edge.leftName != null && isLhsLocked) {
                    node.getParents().put(edge.leftName, lhsPreviousNode);
                    lhsPreviousNode.getChildren().add(node);
                }

                if (edge.rightName != null && isRhsLocked) {
                    node.getParents().put(edge.rightName, rhsPreviousNode);
                    rhsPreviousNode.getChildren().add(node);
                }

                // Register node
                executionGraph.put(node.getNodeId(), node);

                if (edge.leftName != null & !firstNodeByVariable.containsKey(edge.leftName)) {
                    firstNodeByVariable.put(edge.leftName, node);
                }

                if (edge.rightName != null & !firstNodeByVariable.containsKey(edge.rightName)) {
                    firstNodeByVariable.put(edge.rightName, node);
                }
            }
        }

        //printGraph(executionGraph);

        //Nodes Should be in order
        for (ExecutionNode node : executionGraph.values()) {
            node.execute();
        }

        log.info(Utility.buildLog("SCHEDULED", this.pool));

        List<ExecutionNode> leafNodes = executionGraph.values().stream()
                .filter(n -> n.getChildren().isEmpty())
                .toList();

        // Join leaves
        CompletableFuture.allOf(leafNodes.stream()
                .map(ExecutionNode::getFuture)
                .toArray(CompletableFuture[]::new)
        ).join();
        log.info(Utility.buildLog("FINISHED", this.pool));

        List<ResultTable> schema = this.resultFormatter.createResultSchema(executionGraph);

        if(schema.size() > 1) {
            ResultTable joinedTable = this.resultFormatter.join(schema.getFirst(), schema.getLast());
            System.out.println(joinedTable.size());
            //System.out.println(joinedTable);
        }else{
            //schema.getFirst().toFile("b1_holl.txt");
            System.out.println(schema.getFirst().size());
        }

        System.out.println("---------------------------------------------------------------------");
        for(ResultTable table : schema){
            System.out.println("Table: " + table.getColumnNames() +" Size: "+ table.size());
        }
    }

    private List<Edge> getEdgeOrder(Map<Edge, Graph.SetMembership> setMembershipMap) {
        Edge anchor = findAnchorEdge(setMembershipMap);
        List<Edge> remaining = new ArrayList<>(setMembershipMap.keySet());
        remaining.remove(anchor);

        List<Edge> ordered = new ArrayList<>();
        ordered.add(anchor);

        Set<String> seenVariables = new HashSet<>();
        seenVariables.add(anchor.leftName);
        seenVariables.add(anchor.rightName);

        while (!remaining.isEmpty()) {
            boolean found = false;

            for (Iterator<Edge> it = remaining.iterator(); it.hasNext(); ) {
                Edge edge = it.next();

                if (seenVariables.contains(edge.leftName) || seenVariables.contains(edge.rightName)) {
                    ordered.add(edge);
                    seenVariables.add(edge.leftName);
                    seenVariables.add(edge.rightName);
                    it.remove();
                    found = true;
                }
            }

            if (!found) break; // disconnected edges - nothing more reachable
        }

        return ordered;
    }

    private Edge findAnchorEdge(Map<Edge, Graph.SetMembership> map) {
        for (Map.Entry<Edge, Graph.SetMembership> e : map.entrySet()) {
            if (Graph.SetMembership.U.equals(e.getValue())) return e.getKey();
        }

        for (Map.Entry<Edge, Graph.SetMembership> e : map.entrySet()) {
            if (Graph.SetMembership.F.equals(e.getValue())) return e.getKey();
        }

        for (Map.Entry<Edge, Graph.SetMembership> e : map.entrySet()) {
            if (Graph.SetMembership.I_MINUS.equals(e.getValue())) return e.getKey();
        }

        throw new RuntimeException("This query does not support at the moment! Query doesnt have a minimal dependency [U,F,I-]");
    }

    public void shutdownAndAwaitTermination() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Executor Service Shut Down!");
    }
}