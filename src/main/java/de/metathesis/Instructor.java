package de.metathesis;

import de.metathesis.profilers.FDProfiler;
import de.metathesis.profilers.INDProfiler;
import de.metathesis.profilers.UCCProfiler;
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

    public void runPipeline(Map<String, int[]> relationIndexMap) {

        // Start UCC(1)
        UCCRequest uccRequest = new UCCRequest(new SearchSpace.CC(relationIndexMap.get("Y"), 1));
        CompletableFuture<UCCResult> uccFuture = uccProfiler.runAsync(uccRequest);
        List<CompletableFuture<FDResult>> fdFutures = new ArrayList<>();

        int maxLevel = Instructor.max(this.preprocessor.getAttributeSizesOf(relationIndexMap.get("Y")));

        for (int level = 1; level <= maxLevel; level++) {

            final int currentLevel = level;

            // When UCC(L) completes → run IND(L)
            CompletableFuture<INDResult> indFuture = uccFuture.thenCompose(
                    uccResult -> {
                        this.preprocessor.printUCC(uccResult);

                        INDRequest indRequest = new INDRequest(
                                new SearchSpace.CC(relationIndexMap.get("X"), currentLevel),
                                new SearchSpace.Locked(uccResult.asLhsList())
                        );
                        return indProfiler.runAsync(indRequest);
                    }
            );

            CompletableFuture<FDResult> fdFuture = indFuture.thenCompose(
                    indResult -> {
                        this.preprocessor.printIND(indResult);

                        FDRequest fdRequest = new FDRequest(
                                new SearchSpace.Locked(indResult.asRhsList()),
                                new SearchSpace.CC(relationIndexMap.get("Z"), currentLevel)

                        );
                        return fdProfiler.runAsync(fdRequest);
                    }
            );
            fdFutures.add(fdFuture);

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
        uccFuture.join();
        CompletableFuture.allOf(fdFutures.toArray(new CompletableFuture[0])).join();
        System.out.println("Pipeline completed");
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

}