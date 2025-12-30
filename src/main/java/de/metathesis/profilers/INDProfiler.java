package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.results.INDResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public class INDProfiler extends AbstractProfiler<INDRequest, List<INDResult>> {

    public INDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public List<INDResult> profile(INDRequest input) throws InputIterationException {
        this.preprocessor.getPositionListIndexesOf(0);

        for (int i = 1; i <= 5; i++) {
            System.out.println("Profiling IND:" + input.getLevel() + " = " + i);

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
