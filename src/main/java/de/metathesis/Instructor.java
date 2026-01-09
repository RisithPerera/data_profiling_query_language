package de.metathesis;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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

    public void runExecution(List<Edge> orderedEdges, Map<String, int[]> relationIndexMap){
        Map<String, int[]> relationSizesMap = preprocessor.getAttributeSizesOf(relationIndexMap);

        List<ExecutionNode<?, ?>> allNodes = new ArrayList<>();
        Map<Edge, List<ExecutionNode<?, ?>>> lastNodesPerVariable = new HashMap<>();

        for(Edge edge: orderedEdges){
            //I just used instance of to treat each type seperately. Both UCCEdge,FDEdge, INDEdge subclasses for Edge class.

            if(edge instanceof UCCEdge uccEdge){//UCC only have one argument, which also used leftName but there is not rightName
                int[] lhsRelationIndexes = relationIndexMap.get(uccEdge.leftName);
                int[] lhsRelationSizes     = relationSizesMap.get(uccEdge.leftName);

                int maxLevel = max(lhsRelationSizes);

                //This is your previous implementation.
                for (int level = 1; level <= maxLevel; level++) {
                    int[] filteredY = filterRelationsByLevel(lhsRelationIndexes, lhsRelationSizes, level);
                    if (filteredY.length == 0) break;

                    UCCRequest req = new UCCRequest(new SearchSpace.CC(filteredY, level));
                    ExecutionNode<UCCRequest, UCCResult> node = new ExecutionNode<>(uccEdge + "-" + level, uccProfiler, req);

                    // sequential dependency between UCC levels
                    List<ExecutionNode<?, ?>> prev = lastNodesPerVariable.get(uccEdge);
                    if (prev != null && !prev.isEmpty()) {
                        node.dependsOn.add(prev.get(prev.size() - 1));
                    }

                    lastNodesPerVariable.computeIfAbsent(uccEdge, k -> new ArrayList<>()).add(node);

                    allNodes.add(node);
                }
            }else if(edge instanceof FDEdge fdEdge){
                int[] lhsRelationIndexes = relationIndexMap.get(fdEdge.leftName);
                int[] lhsRelationSizes     = relationSizesMap.get(fdEdge.leftName);

                int[] rhsRelationIndexes = relationIndexMap.get(fdEdge.rightName);
                int[] rhsRelationSizes     = relationSizesMap.get(fdEdge.rightName);

                //You have to figure it out
            }else if(edge instanceof INDEdge indEdge){
                int[] lhsRelationIndexes = relationIndexMap.get(indEdge.leftName);
                int[] lhsRelationSizes     = relationSizesMap.get(indEdge.leftName);

                int[] rhsRelationIndexes = relationIndexMap.get(indEdge.rightName);
                int[] rhsRelationSizes     = relationSizesMap.get(indEdge.rightName);
            }
        }

        System.out.println(allNodes);
    }

    public void runPipeline(Map<String, int[]> relationIndexMap) {
        // Start UCC(1)
        UCCRequest uccRequest = new UCCRequest(new SearchSpace.CC(relationIndexMap.get("Y"), 1));
        CompletableFuture<UCCResult> uccFuture = uccProfiler.runAsync(uccRequest);
        List<CompletableFuture<FDResult>> fdFutures = new ArrayList<>();

        int maxLevel = max(this.preprocessor.getAttributeSizesOf(relationIndexMap.get("Y")));

        //Temporary Collecting Results
        List<UCCResult.UCC> uccResults = new ArrayList<>();
        List<INDResult.IND> indResults = new ArrayList<>();
        List<FDResult.FD> fdResults = new ArrayList<>();

        for (int level = 1; level <= maxLevel; level++) {

            final int currentLevel = level;

            // When UCC(L) completes → run IND(L)
            CompletableFuture<INDResult> indFuture = uccFuture.thenCompose(
                    uccResult -> {
                        Instructor.printLog(String.format("F: UCC     L:%d", currentLevel), this.pool);
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
                        Instructor.printLog(String.format("F: IND     L:%d", currentLevel), this.pool);
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
                    Instructor.printLog(String.format("F: FD      L:%d", currentLevel), this.pool);
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

    public void runUCCOnly(Map<String, int[]> relationIndexMap) {
        // Start UCC(1)
        UCCRequest uccRequest = new UCCRequest(new SearchSpace.CC(relationIndexMap.get("X"), 1));
        CompletableFuture<UCCResult> uccFuture = uccProfiler.runAsync(uccRequest);

        int maxLevel = Instructor.max(this.preprocessor.getAttributeSizesOf(relationIndexMap.get("X")));

        //Temporary Collecting Results
        List<UCCResult.UCC> uccResults = new ArrayList<>();

        for (int level = 1; level <= maxLevel; level++) {

            final int currentLevel = level;

            uccFuture.thenAccept( uccResult -> {
                        Instructor.printLog(String.format("F: FD      L:%d", currentLevel), this.pool);
                        uccResult.forEach(uccResults::add);
                    }
            );

            // Prepare UCC(L+1) immediately after UCC(L)
            if (level < maxLevel) {
                uccFuture = uccFuture.thenCompose(
                        ignored -> uccProfiler.runAsync(
                                new UCCRequest(new SearchSpace.CC(relationIndexMap.get("X"), currentLevel + 1))
                        )
                );
            }
        }

        System.out.println("Pipeline scheduled");
        uccFuture.join();
        System.out.println("Pipeline completed");

        for(UCCResult.UCC ucc : uccResults) {
            this.preprocessor.printAttributeSet(ucc.lhs);
        }
    }

    public void runINDOnly(Map<String, int[]> relationIndexMap) {
        // Start UCC(1)
        UCCRequest uccRequest = new UCCRequest(new SearchSpace.CC(relationIndexMap.get("X"), 1));
        CompletableFuture<UCCResult> uccFuture = uccProfiler.runAsync(uccRequest);

        int maxLevel = Instructor.max(this.preprocessor.getAttributeSizesOf(relationIndexMap.get("X")));

        //Temporary Collecting Results
        List<UCCResult.UCC> uccResults = new ArrayList<>();

        for (int level = 1; level <= maxLevel; level++) {

            final int currentLevel = level;

            uccFuture.thenAccept( uccResult -> {
                        Instructor.printLog(String.format("F: FD      L:%d", currentLevel), this.pool);
                        uccResult.forEach(uccResults::add);
                    }
            );

            // Prepare UCC(L+1) immediately after UCC(L)
            if (level < maxLevel) {
                uccFuture = uccFuture.thenCompose(
                        ignored -> uccProfiler.runAsync(
                                new UCCRequest(new SearchSpace.CC(relationIndexMap.get("X"), currentLevel + 1))
                        )
                );
            }
        }

        System.out.println("Pipeline scheduled");
        uccFuture.join();
        System.out.println("Pipeline completed");

        for(UCCResult.UCC ucc : uccResults) {
            this.preprocessor.printAttributeSet(ucc.lhs);
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

    static int[] filterRelationsByLevel(int[] relations, int[] sizes, int level) {
        List<Integer> filtered = new ArrayList<>();
        for (int i = 0; i < relations.length; i++) {
            if (sizes[i] >= level) {
                filtered.add(relations[i]);
            }
        }
        return filtered.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int max(int[] sizes) {
        int m = 0;
        for (int s : sizes) m = Math.max(m, s);
        return m;
    }


    public static void printLog(String tag, Executor pool){
        // 1. Define your executor
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) pool;

        // 2. Later in your code, or in a background "Monitor" thread:
        System.out.printf(
                "[%s] [%d/%d] Active: %d, Completed: %d, Queue: %d%n",
                tag,
                threadPool.getPoolSize(),
                threadPool.getMaximumPoolSize(),
                threadPool.getActiveCount(),
                threadPool.getCompletedTaskCount(),
                threadPool.getQueue().size()
        );
    }
}