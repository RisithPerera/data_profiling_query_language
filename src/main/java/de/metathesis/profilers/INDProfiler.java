package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.INDResult;

import java.util.concurrent.Executor;

public class INDProfiler extends AbstractProfiler<INDRequest, INDResult> {

    public INDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public INDResult profile(INDRequest input) throws InputIterationException {
        this.preprocessor.getPositionListIndexesOf(0);

        INDResult result = new INDResult();
        for (int i = 1; i <= 5; i++) {
            if(input.rhs() instanceof SearchSpace.CC cc){
                System.out.println("Profiling  IND:" + cc.level() + " = " + i);
            }

            try {
                // Pause the execution for 1 second
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result;
    }
}
