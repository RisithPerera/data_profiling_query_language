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
        FDResult result = new FDResult();

        if(input.lhs() instanceof SearchSpace.Locked locked && input.rhs() instanceof SearchSpace.CC cc) {
            for(int relationIndex : cc.relations()){
                System.out.println("Profiling  FD  -> Relation: " + relationIndex + " Level: "+ cc.level());
                if(cc.level() == 1){

                }else{

                }

            }
        }
        return result;
    }
}

