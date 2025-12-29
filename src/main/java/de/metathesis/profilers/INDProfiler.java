package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.results.INDResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class INDProfiler extends AbstractProfiler<List<INDResult>> {

    public INDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public CompletableFuture<List<INDResult>> runAsync(int level) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return profile(level);
                    } catch (InputIterationException e) {
                        throw new RuntimeException(e);
                    }
                },
                executor
        );
    }

    @Override
    public List<INDResult> profile(int number) throws InputIterationException {
        this.preprocessor.getPositionListIndexesOf(0);
        for (int i = 1; i <= 5; i++) {
            System.out.println("Profiling IND:" + number + " = " + i);

            try {
                // Pause the execution for 1 second
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Collections.emptyList();
    }
}
