package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Utility;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
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
        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            for(int rhsRelationIndex : rhs.relations()){
                for(int lhsRelationIndex : lhs.relations()){
                    if(lhsRelationIndex != rhsRelationIndex){
                        continue;
                    }

                    if(rhs.level() == 1){
                        PositionListIndex[] plis = this.preprocessor.getPositionListIndexesOf(lhsRelationIndex);

                        for (PositionListIndex lhsPli : plis) {
                            for (PositionListIndex rhsPli : plis) {
                                //If the rhsPli does not split any partitions of the lhsPli, the FD is valid!
                                PositionListIndex pli = lhsPli.intersect(rhsPli);
                                if(lhsPli.equals(pli)){
                                    result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                                }
                            }
                        }
                    }else{
                        throw new UnsupportedOperationException("Not supported yet! LHS=CC, RHS=CC, Level > 1");
                    }
                }
            }
        }else if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            throw new UnsupportedOperationException("LHS=CC RHS=Locked");
        }else if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.CC rhs){
            for(int rhsRelationIndex : rhs.relations()){
                for(AttributeBitSet lhsAttributeSet : lhs.attributes()){
                    if(lhsAttributeSet.getRelationIndex() != rhsRelationIndex){
                        continue;
                    }
                    Utility.printLog(String.format("P: FD R:%d L:%d", rhsRelationIndex,  rhs.level()), this.executor);
                    PositionListIndex[] plis = this.preprocessor.getPositionListIndexesOf(rhsRelationIndex);

                    for (PositionListIndex lhsPli : plis) {
                        if(lhsPli.getAttributeSet().equals(lhsAttributeSet)){
                            for (PositionListIndex rhsPli : plis) {
                                if(lhsPli.equals(rhsPli)) continue;

                                //If the rhsPli does not split any partitions of the lhsPli, the FD is valid!
                                PositionListIndex intersectPli = lhsPli.intersect(rhsPli);
                                if(lhsPli.isEqualClusters(intersectPli)){
                                    result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                                }
                            }
                        }
                    }
                }
            }
        }else if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            throw new UnsupportedOperationException("LHS=Locked RHS=Locked");
        }

        return result;
    }
}

