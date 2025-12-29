package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.results.FDResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FDProfiler extends AbstractProfiler<List<FDResult>> {

    public FDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public CompletableFuture<List<FDResult>> runAsync(int level) {
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
    public List<FDResult> profile(int number) throws InputIterationException {
        this.preprocessor.getPositionListIndexesOf(0);
        for (int i = 1; i <= 5; i++) {
            System.out.println("Profiling  FD:" + number + " = " + i);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Collections.emptyList();
    }
}

