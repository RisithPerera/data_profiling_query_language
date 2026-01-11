package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Utility;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.FDResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.concurrent.Executor;

public class FDProfiler extends AbstractProfiler<FDRequest, FDResult> {

    public FDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public FDResult profile(FDRequest input) throws InputIterationException {
        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileF(lhs.relations(), rhs.relations(), lhs.level());
        }

        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            return profileF_VALID(lhs.relations(), rhs.attributes(), lhs.level());
        }

        if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.CC rhs){
            return profileF_PLUS(lhs.attributes(), rhs.relations());
        }

        if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            return profileF_PLUS_VALID(lhs.attributes(), rhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported FDRequest");
    }

    private FDResult profileF(int[] lhsRelationIndexes,
                              int[] rhsRelationIndexes,
                              int level) throws InputIterationException {
        FDResult result = new FDResult();
        int[] commonRelationIndexes = Utility.intersect(lhsRelationIndexes, rhsRelationIndexes);
        for(int relationIndex : commonRelationIndexes) {
            AttributeBitSet[] currentLevel = this.preprocessor.generateApriori(relationIndex, level);
            AttributeBitSet[] initialLevel = this.preprocessor.generateApriori(relationIndex, 1);

            for (AttributeBitSet lhsAbs : currentLevel) {
                PositionListIndex lhsPli = this.preprocessor.getPLI(lhsAbs);
                if(lhsPli.isUnique()){
                    //TODO:Avoiding unique LHS, Needs to discuss this team
                    continue;
                }

                for (AttributeBitSet rhsAbs : initialLevel) {
                    if(lhsAbs.equals(rhsAbs) || rhsAbs.isSubsetOf(lhsAbs)){ //for triviality pruning
                        continue;
                    }

                    PositionListIndex rhsPli = this.preprocessor.getPLI(rhsAbs);

                    if(isFD(lhsPli, rhsPli)){
                        result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                    }
                }
            }
        }

        return  result;
    }

    private FDResult profileF_PLUS(ObjectOpenHashSet<AttributeBitSet> lhsAttributes,
                                   int[] rhsRelationIndexes) throws InputIterationException {
        FDResult result = new FDResult();
        for (AttributeBitSet lhsAbs : lhsAttributes) {
            PositionListIndex lhsPli = this.preprocessor.getPLI(lhsAbs);
            if(lhsPli.isUnique()){
                //TODO:Avoiding unique LHS, Needs to discuss this team
                continue;
            }

            for(int rhsRelationIndex : rhsRelationIndexes) {
                if(rhsRelationIndex != lhsAbs.getRelationIndex()){ //Relation Matching
                    continue;
                }

                AttributeBitSet[] initialLevel = this.preprocessor.generateApriori(rhsRelationIndex, 1); //Get Level 1 Rhs

                for (AttributeBitSet rhsAbs : initialLevel) {
                    if(lhsAbs.equals(rhsAbs) || rhsAbs.isSubsetOf(lhsAbs)){ //for triviality pruning
                        continue;
                    }

                    PositionListIndex rhsPli = this.preprocessor.getPLI(rhsAbs);

                    if(isFD(lhsPli, rhsPli)){
                        result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                    }
                }
            }
        }
        return  result;
    }

    private FDResult profileF_VALID(int[] lhsRelationIndexes,
                                    ObjectOpenHashSet<AttributeBitSet> rhsAttributes,
                                    int level) throws InputIterationException {
        FDResult result = new FDResult();

        for (AttributeBitSet rhsAbs : rhsAttributes) {
            PositionListIndex rhsPli = this.preprocessor.getPLI(rhsAbs);

            for(int lhsRelationIndex : lhsRelationIndexes) {
                if(lhsRelationIndex != rhsAbs.getRelationIndex()){ //Relation Matching
                    continue;
                }

                AttributeBitSet[] currentLevel = this.preprocessor.generateApriori(lhsRelationIndex, level);

                for (AttributeBitSet lhsAbs : currentLevel) {
                    if(lhsAbs.equals(rhsAbs) || rhsAbs.isSubsetOf(lhsAbs)){ //for triviality pruning
                        continue;
                    }

                    PositionListIndex lhsPli = this.preprocessor.getPLI(lhsAbs);
                    if(lhsPli.isUnique()){
                        //TODO:Avoiding unique LHS, Needs to discuss this team
                        continue;
                    }

                    if(isFD(lhsPli, rhsPli)){
                        result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                    }
                }
            }
        }

        return  result;
    }

    private FDResult profileF_PLUS_VALID(ObjectOpenHashSet<AttributeBitSet> lhsAttributes,
                                         ObjectOpenHashSet<AttributeBitSet> rhsAttributes) throws InputIterationException {
        FDResult result = new FDResult();

        for(AttributeBitSet lhsAbs : lhsAttributes) {
            PositionListIndex lhsPli = this.preprocessor.getPLI(lhsAbs);
            if(lhsPli.isUnique()){
                //TODO:Avoiding unique LHS, Needs to discuss this team
                continue;
            }

            for (AttributeBitSet rhsAbs : rhsAttributes) {
                if(lhsAbs.getRelationIndex() != rhsAbs.getRelationIndex()){ //Relation Matching
                    continue;
                }

                if(lhsAbs.equals(rhsAbs) || rhsAbs.isSubsetOf(lhsAbs)){ //for triviality pruning
                    continue;
                }

                PositionListIndex rhsPli = this.preprocessor.getPLI(rhsAbs);

                if(isFD(lhsPli, rhsPli)){
                    result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                }

            }
        }

        return result;
    }

    private boolean isFD(PositionListIndex lhsPli, PositionListIndex rhsPli){
        AttributeBitSet abs = lhsPli.getAttributeSet().union(rhsPli.getAttributeSet());
        PositionListIndex intersectedPli = preprocessor.getOrComputePLI(abs, () -> lhsPli.intersect(rhsPli));

        //If the rhsPli does not split any partitions of the lhsPli, the FD is valid!
        if (lhsPli.getClusters().equals(intersectedPli.getClusters())) {
            System.out.println(lhsPli.getAttributeSet() + "," + rhsPli.getAttributeSet());
            return true;
        }

        return false;
    }
}

