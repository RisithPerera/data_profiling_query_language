package de.metathesis;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metaserve.executor.min.graph.Graph;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.edge.FDEdge;
import de.metaserve.executor.min.graph.edge.INDEdge;
import de.metaserve.executor.min.graph.edge.UCCEdge;
import de.metathesis.profilers.FDProfiler;
import de.metathesis.profilers.INDProfiler;
import de.metathesis.profilers.UCCProfiler;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.ExecutionNode;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class Instructor {

    private static volatile Instructor INSTANCE;
    private final ExecutorService pool;
    private final Preprocessor preprocessor;
    private final UCCProfiler uccProfiler;
    private final INDProfiler indProfiler;
    private final FDProfiler fdProfiler;

    private Instructor(ExecutorService pool) {
        this.pool = pool;
        this.preprocessor = Preprocessor.getInstance();
        this.uccProfiler = new UCCProfiler(pool);
        this.indProfiler = new INDProfiler(pool);
        this.fdProfiler  = new FDProfiler(pool);
    }

    public static Instructor getInstance(ExecutorService pool) {
        if (INSTANCE == null) {
            INSTANCE = new Instructor(pool);
        }

        return INSTANCE;
    }

    public void runExecution(Map<Edge, Graph.SetMembership> setMembershipMap, Map<String, List<String>> relationMap) throws InputGenerationException, AlgorithmConfigurationException {
        Map<String, int[]> relationIndexMap = this.preprocessor.initializeSearchSpace(relationMap);
        Map<String, int[]> relationSizesMap = this.preprocessor.getAttributeSizesOf(relationIndexMap);

        //Make an order list of dependencies based on set membership priority order
        List<Edge> orderedEdges = setMembershipMap.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().priority()))
                .map(Map.Entry::getKey)
                .toList();

        int globalMaxLevel = Utility.max(relationSizesMap.values());

        Map<String, ExecutionNode<?, ?>> executionGraph = new LinkedHashMap<>(); //All Graph Nodes

        for (int level = 1; level <= globalMaxLevel; level++) {
            final int currentLevel = level;

            // Tracks last Execution Node that constrained a variable
            Map<String, ExecutionNode<?, ?>> lastNodeByVariable = new HashMap<>();

            for (Edge edge : orderedEdges) {
                final int[] lhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get(edge.leftName), relationSizesMap.get(edge.leftName), level);
                final int[] rhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get(edge.rightName), relationSizesMap.get(edge.rightName), level);

                final boolean isLhsLocked = lastNodeByVariable.containsKey(edge.leftName);
                final boolean isRhsLocked = lastNodeByVariable.containsKey(edge.rightName);

                final ExecutionNode<?, ?> lhsPreviousNode = lastNodeByVariable.get(edge.leftName);
                final ExecutionNode<?, ?> rhsPreviousNode = lastNodeByVariable.get(edge.rightName);

                ExecutionNode<?, ?> node = null;

                if (edge instanceof UCCEdge) {
                    node = new ExecutionNode<>(
                            edge,
                            currentLevel,
                            uccProfiler,
                            results -> {
                                SearchSpace lhsSearchSpace = isLhsLocked
                                        ? new SearchSpace.Locked(results.get(lhsPreviousNode).asLhsSet())
                                        : new SearchSpace.CC(lhsRelationIndexes, currentLevel);

                                return new UCCRequest(lhsSearchSpace);
                            }
                    );

                } else if (edge instanceof INDEdge) {
                    node = new ExecutionNode<>(
                            edge,
                            currentLevel,
                            indProfiler,
                            results -> {
                                SearchSpace lhsSearchSpace = isLhsLocked
                                        ? new SearchSpace.Locked(results.get(lhsPreviousNode).asLhsSet())
                                        : new SearchSpace.CC(lhsRelationIndexes, currentLevel);

                                SearchSpace rhsSearchSpace = isRhsLocked
                                        ? new SearchSpace.Locked(results.get(rhsPreviousNode).asRhsSet())
                                        : new SearchSpace.CC(rhsRelationIndexes, currentLevel);

                                return new INDRequest(lhsSearchSpace, rhsSearchSpace);
                            }
                    );
                } else if (edge instanceof FDEdge) {
                    node = new ExecutionNode<>(
                            edge,
                            currentLevel,
                            fdProfiler,
                            results -> {
                                SearchSpace lhsSearchSpace = isLhsLocked
                                        ? new SearchSpace.Locked(results.get(lhsPreviousNode).asLhsSet())
                                        : new SearchSpace.CC(lhsRelationIndexes, currentLevel);

                                SearchSpace rhsSearchSpace = isRhsLocked
                                        ? new SearchSpace.Locked(results.get(rhsPreviousNode).asRhsSet())
                                        : new SearchSpace.CC(rhsRelationIndexes, currentLevel);

                                return new FDRequest(lhsSearchSpace, rhsSearchSpace);
                            }
                    );
                }

                if (node == null) continue;

                // Dependency: previous level of same edge
                if (!(isRhsLocked || isLhsLocked) && executionGraph.containsKey(node.getPreviousNodeId())) {
                    node.getParents().add(executionGraph.get(node.getPreviousNodeId()));
                }

                // Dependency: last use of variables
                if (edge.leftName != null && isLhsLocked) {
                    node.getParents().add(lastNodeByVariable.get(edge.leftName));
                    lastNodeByVariable.get(edge.leftName).getChildren().add(node);
                }

                if (edge.rightName != null && isRhsLocked) {
                    node.getParents().add(lastNodeByVariable.get(edge.rightName));
                    lastNodeByVariable.get(edge.rightName).getChildren().add(node);
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

        //Nodes Should be in order
        for (ExecutionNode<?, ?> node : executionGraph.values()) {
            node.execute();
        }

        Utility.printLog("SCHEDULED", this.pool);

        List<ExecutionNode<?, ?>> leafNodes = executionGraph.values().stream()
                .filter(n -> n.getChildren().isEmpty())
                .toList();

        // Join leaves
        CompletableFuture.allOf(leafNodes.stream()
                .map(ExecutionNode::getFuture)
                .toArray(CompletableFuture[]::new)
        ).join();
        Utility.printLog("FINISH", this.pool);

        Map<Edge, List<de.metanome.algorithm_integration.results.Result>> results = collectResults(executionGraph);

        for(Edge edge : orderedEdges){
            edge.setResults(results.get(edge));
        }
    }

    private Map<Edge, List<de.metanome.algorithm_integration.results.Result>> collectResults(Map<String, ExecutionNode<?, ?>> executionGraph) {
        Map<Edge, List<de.metanome.algorithm_integration.results.Result>> results = new HashMap<>();

        if (executionGraph == null) return results;

        for (ExecutionNode<?, ?> node : executionGraph.values()) {
            if(node.getResults().isEmpty()){
                continue;
            }

            if (!results.containsKey(node.getEdge())) {
                results.put(node.getEdge(), new ArrayList<>());
            }

            for(Object x:  node.getResults()) {
                if(x instanceof UCCResult.UCC ucc){
                    results.get(node.getEdge()).add(this.preprocessor.formatUCC(ucc));
                }else if(x instanceof INDResult.IND ind){
                    results.get(node.getEdge()).add(this.preprocessor.formatIND(ind));
                }else if(x instanceof FDResult.FD fd){
                    results.get(node.getEdge()).add(this.preprocessor.formatFD(fd));
                }
            }
        }

        return results;
    }

    private void collectResultsFromLeaf(ExecutionNode<?, ?> node, Map<String, ObjectOpenHashSet<AttributeBitSet>> accumulated) {
        if(node.getResults().isEmpty()){
            return;
        }

        if(node.getParents().isEmpty()){
            return;
        }

        if(!accumulated.containsKey(node.getEdge().leftName) && !accumulated.containsKey(node.getEdge().rightName)){
            accumulated.put(node.getEdge().leftName, node.getResults().asLhsSet());
            accumulated.put(node.getEdge().rightName, node.getResults().asRhsSet());
        }

        if(accumulated.containsKey(node.getEdge().leftName) && !accumulated.containsKey(node.getEdge().rightName)){
            ObjectOpenHashSet<AttributeBitSet> newVariable =  new ObjectOpenHashSet<>();
            for(AttributeBitSet abs: accumulated.get(node.getEdge().leftName)){
                for(AttributeBitSet xx: node.getResults().lhs()){
                    if(abs.equals(xx)){
                        System.out.println(abs);
                    }
                }

            }
            accumulated.put(node.getEdge().rightName, newVariable);
        }


        for (ExecutionNode<?, ?> grandParent : node.getParents()) {
            collectResultsFromLeaf(grandParent, accumulated);
        }
    }

    public void runPipeline(Map<String, int[]> relationIndexMap) {
        // Start UCC(1)
        UCCRequest uccRequest = new UCCRequest(new SearchSpace.CC(relationIndexMap.get("Y"), 1));
        CompletableFuture<UCCResult> uccFuture = uccProfiler.runAsync(uccRequest);
        List<CompletableFuture<FDResult>> fdFutures = new ArrayList<>();

        int maxLevel = Utility.max(this.preprocessor.getAttributeSizesOf(relationIndexMap.get("Y")));

        //Temporary Collecting Results
        List<UCCResult.UCC> uccResults = new ArrayList<>();
        List<INDResult.IND> indResults = new ArrayList<>();
        List<FDResult.FD> fdResults = new ArrayList<>();

        for (int level = 1; level <= maxLevel; level++) {

            final int currentLevel = level;

            // When UCC(L) completes → run IND(L)
            CompletableFuture<INDResult> indFuture = uccFuture.thenCompose(
                    uccResult -> {
                        Utility.printLog(String.format("F: UCC     L:%d", currentLevel), this.pool);
                        //this.preprocessor.printUCC(uccResult);
                        uccResult.forEach(uccResults::add);
                        INDRequest indRequest = new INDRequest(
                                new SearchSpace.CC(relationIndexMap.get("X"), currentLevel),
                                new SearchSpace.Locked(uccResult.asLhsSet())
                        );
                        return indProfiler.runAsync(indRequest);
                    }
            );

            CompletableFuture<FDResult> fdFuture = indFuture.thenCompose(
                    indResult -> {
                        Utility.printLog(String.format("F: IND     L:%d", currentLevel), this.pool);
                        //this.preprocessor.printIND(indResult);
                        indResult.forEach(indResults::add);
                        FDRequest fdRequest = new FDRequest(
                                new SearchSpace.Locked(indResult.asLhsSet()),
                                new SearchSpace.CC(relationIndexMap.get("Z"), currentLevel)

                        );
                        return fdProfiler.runAsync(fdRequest);
                    }
            );

            fdFuture.thenAccept( fdResult -> {
                Utility.printLog(String.format("F: FD      L:%d", currentLevel), this.pool);
                    //this.preprocessor.printFD(fdResult);
                    fdResult.forEach(fdResults::add);
                }
            );
            fdFutures.add(fdFuture); //Collecting Leaf Nodes

            // Prepare UCC(L+1) immediately after UCC(L)
            if (level < maxLevel) {
                uccFuture = uccFuture.thenCompose(
                                ignored -> uccProfiler.runAsync(
                                        new UCCRequest(new SearchSpace.CC(relationIndexMap.get("Y"), currentLevel + 1))
                                )
                        );
            }
        }

        System.out.println("Pipeline scheduled");
        CompletableFuture.allOf(fdFutures.toArray(new CompletableFuture[0])).join();
        System.out.println("Pipeline completed");
        for(FDResult.FD fd : fdResults){
            for(INDResult.IND ind : indResults) {
                if(fd.lhs.equals(ind.lhs)){
                    this.preprocessor.printAttributeSet(fd.lhs, ind.rhs, fd.rhs);
//                    for(UCCResult.UCC ucc : uccResults) {
//                        if(ind.rhs.equals(ucc.lhs)){
//
//                        }
//                    }
                }
            }
        }
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
    }
}