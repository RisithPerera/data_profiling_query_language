package de.metathesis;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.edge.FDEdge;
import de.metaserve.executor.min.graph.edge.INDEdge;
import de.metaserve.executor.min.graph.edge.UCCEdge;
import de.metathesis.profilers.ProfilerFactory;
import de.metathesis.profilers.results.FDResult;
import de.metathesis.profilers.results.INDResult;
import de.metathesis.profilers.results.Result;
import de.metathesis.profilers.results.UCCResult;
import de.metathesis.structures.ExecutionNode;
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
    private final ProfilingContext profilingContext;
    private final ProfilerFactory profilerFactory;

    public Instructor(ExecutorService pool) {
        this.pool = pool;
        this.profilingContext = ProfilingContext.getInstance();
        this.profilerFactory = new ProfilerFactory(pool);
    }

    public Map<Edge, Result<?>> runExecutionMethod1(Map<Edge, Graph.SetMembership> setMembershipMap, Map<String, List<String>> relationMap) throws InputGenerationException, AlgorithmConfigurationException {
        Map<String, int[]> relationIndexMap = this.profilingContext.initializeSearchSpace(relationMap);
        Map<String, int[]> relationSizesMap = this.profilingContext.getAttributeSizesOf(relationIndexMap);

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

        return collectResults(executionGraph);
    }

    public Map<Edge, Result<?>> runExecutionMethod2(Map<Edge, Graph.SetMembership> setMembershipMap, Map<String, List<String>> relationMap) throws InputGenerationException, AlgorithmConfigurationException {
        Map<String, int[]> relationIndexMap = this.profilingContext.initializeSearchSpace(relationMap);
        Map<String, int[]> relationSizesMap = this.profilingContext.getAttributeSizesOf(relationIndexMap);

        //Make an order list of dependencies based on set membership priority order
        List<Edge> orderedEdges = getEdgeOrder(setMembershipMap);

        int globalMaxLevel = Utility.max(relationSizesMap.values());

        Map<String, ExecutionNode> executionGraph = new LinkedHashMap<>(); //All Graph Nodes

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

        return collectResults(executionGraph);
    }

    private List<Edge> getEdgeOrder(Map<Edge, Graph.SetMembership> setMembershipMap) {
        List<Edge> ordered = new ArrayList<>();
        List<Edge> remaining = new ArrayList<>(setMembershipMap.keySet());
        Set<String> lockedVariables = new HashSet<>();

        List<List<Graph.SetMembership>> priorityLevels = List.of(
                List.of(Graph.SetMembership.U, Graph.SetMembership.F),
                List.of(Graph.SetMembership.U_PLUS, Graph.SetMembership.F_PLUS),
                List.of(Graph.SetMembership.I_MINUS, Graph.SetMembership.I)
        );

        //Finding anchor nodes
        for (List<Graph.SetMembership> level : priorityLevels) {
            for (Iterator<Edge> it = remaining.iterator(); it.hasNext(); ) {
                Edge edge = it.next();
                if (level.contains(setMembershipMap.get(edge))) {
                    ordered.add(edge);
                    lockedVariables.add(edge.leftName);
                    lockedVariables.add(edge.rightName);
                    it.remove();
                }
            }

            if (!ordered.isEmpty()) break;
        }

        //Finding connected nodes
        while (!remaining.isEmpty()) {
            boolean found = false;
            for (Iterator<Edge> it = remaining.iterator(); it.hasNext(); ) {
                Edge edge = it.next();
                if (lockedVariables.contains(edge.leftName) || lockedVariables.contains(edge.rightName)) {
                    ordered.add(edge);
                    lockedVariables.add(edge.leftName);
                    lockedVariables.add(edge.rightName);
                    it.remove();
                    found = true;
                }
            }
            if (!found){
                ordered.addAll(remaining);
                break;
            }
        }

        return ordered;
    }

    private Map<Edge, Result<?>> collectResults(Map<String, ExecutionNode> executionGraph){
        Map<Edge, Result<?>> resultsByEdge = new LinkedHashMap<>();

        for (ExecutionNode node : executionGraph.values()) {
            Edge edge = node.getEdge();

            resultsByEdge.computeIfAbsent(edge, k -> {
                if (k instanceof FDEdge) return new FDResult();
                if (k instanceof INDEdge) return new INDResult();
                if (k instanceof UCCEdge) return new UCCResult();
                throw new IllegalStateException("Unknown edge type: " + k);
            });

            Result<?> result = resultsByEdge.get(edge);

            for (Object x : node.getResults()) {
                if (x instanceof FDResult.FD fd && result instanceof FDResult fdResult) {
                    fdResult.add(fd.lhs, fd.rhs); // dedup handled inside
                } else if (x instanceof INDResult.IND ind && result instanceof INDResult indResult) {
                    indResult.add(ind.lhs, ind.rhs);
                } else if (x instanceof UCCResult.UCC ucc && result instanceof UCCResult uccResult) {
                    uccResult.add(ucc.lhs);
                }
            }
        }

        return resultsByEdge;
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