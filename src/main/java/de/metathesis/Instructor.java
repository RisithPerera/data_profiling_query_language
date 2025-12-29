package de.metathesis;

import de.metathesis.profilers.FDProfiler;
import de.metathesis.profilers.INDProfiler;
import de.metathesis.profilers.UCCProfiler;
import de.metathesis.structures.ImmutableBitSet;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.UCCResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class Instructor {

    private static volatile Instructor INSTANCE;
    private final ExecutorService pool;
    private final UCCProfiler uccProfiler;
    private final INDProfiler indProfiler;
    private final FDProfiler fdProfiler;

    private Instructor(ExecutorService pool) {
        this.pool = pool;

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

    public void runPipeline(int maxLevel) {

        // Start UCC(1)
        CompletableFuture<List<UCCResult>> uccFuture = uccProfiler.runAsync(1);
        List<CompletableFuture<List<FDResult>>> fdFutures = new ArrayList<>();

        for (int level = 1; level <= maxLevel; level++) {

            final int currentLevel = level;

            // When UCC(L) completes → run IND(L)
            CompletableFuture<List<INDResult>> indFuture = uccFuture.thenCompose(
                    uccResult -> {
                        System.out.println(uccResult);
                        return indProfiler.runAsync(currentLevel);
                    }
            );

            CompletableFuture<List<FDResult>> fdFuture = indFuture.thenCompose(
                    ignored -> fdProfiler.runAsync(currentLevel)
            );
            fdFutures.add(fdFuture);

            // Prepare UCC(L+1) immediately after UCC(L)
            if (level < maxLevel) {
                uccFuture = uccFuture.thenCompose(
                                ignored -> uccProfiler.runAsync(currentLevel + 1)
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
}