package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.FDResult;

import java.util.concurrent.Executor;

public class FDProfiler extends AbstractProfiler<FDRequest, FDResult> {

    public FDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public FDResult profile(FDRequest input) throws InputIterationException {
        this.preprocessor.getPositionListIndexesOf(0);

        FDResult result = new FDResult();
        for (int i = 1; i <= 5; i++) {
            if(input.lhs() instanceof SearchSpace.CC cc){
                System.out.println("Profiling  FD:" + cc.level() + " = " + i);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result;
    }
}

