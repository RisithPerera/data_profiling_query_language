package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.results.FDResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public class FDProfiler extends AbstractProfiler<FDRequest, List<FDResult>> {

    public FDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public List<FDResult> profile(FDRequest input) throws InputIterationException {
        this.preprocessor.getPositionListIndexesOf(0);
        for (int i = 1; i <= 5; i++) {
            System.out.println("Profiling  FD:" + input.getLevel() + " = " + i);

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

