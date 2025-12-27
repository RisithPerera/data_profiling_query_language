package de.metathesis;

import de.metaserve.parser.graph.FD;
import de.metathesis.profilers.AbstractProfiler;
import de.metathesis.profilers.FDProfiler;
import de.metathesis.profilers.INDProfiler;
import de.metathesis.profilers.UCCProfiler;
import de.metathesis.structures.AttributeList;

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
        CompletableFuture<AttributeList[]> uccFuture = uccProfiler.runAsync(1);

        for (int level = 0; level < maxLevel; level++) {

            final int currentLevel = level;

            // When UCC(L) completes → run IND(L)
            CompletableFuture<AttributeList[]> indFuture = uccFuture.thenCompose(
                    uccResult -> indProfiler.runAsync(currentLevel)
            );

            CompletableFuture<Void> fdFuture = indFuture.thenAccept(
                    ignored -> fdProfiler.runAsync(currentLevel)
            );

            // Prepare UCC(L+1) immediately after UCC(L)
            if (level < maxLevel) {
                uccFuture = uccFuture.thenCompose(
                                ignored -> uccProfiler.runAsync(currentLevel + 1)
                        );
            }
        }

        System.out.println("Pipeline scheduled");
        uccFuture.join();
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