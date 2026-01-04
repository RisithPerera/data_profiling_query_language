package de.metathesis;

import de.metathesis.profilers.FDProfiler;
import de.metathesis.profilers.INDProfiler;
import de.metathesis.profilers.UCCProfiler;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.UCCResult;

import java.util.ArrayList;
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

    public void runPipeline(Map<String, int[]> relationIndexMap) {

        // Start UCC(1)
        UCCRequest uccRequest = new UCCRequest(new SearchSpace.CC(relationIndexMap.get("Y"), 1));
        CompletableFuture<UCCResult> uccFuture = uccProfiler.runAsync(uccRequest);
        List<CompletableFuture<FDResult>> fdFutures = new ArrayList<>();

        int maxLevel = Instructor.max(this.preprocessor.getAttributeSizesOf(relationIndexMap.get("Y")));

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
                    for(UCCResult.UCC ucc : uccResults) {
                        if(ind.rhs.equals(ucc.lhs)){
                            this.preprocessor.printAttributeSet(fd.lhs, ucc.lhs, fd.rhs);
                        }
                    }
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

    private static int max(int[] a) {
        int m = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] > m) m = a[i];
        }
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