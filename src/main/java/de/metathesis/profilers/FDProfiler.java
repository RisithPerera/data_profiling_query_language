package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.NegativeCover;
import de.metathesis.Sampler2;
import de.metathesis.Utility;
import de.metathesis.ValidatorNew;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.FDResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class FDProfiler extends AbstractProfiler<FDRequest, FDResult> {

    private final Int2ObjectMap<Map<Integer, List<BitSet>>> confirmedPosCover = new Int2ObjectOpenHashMap<>();

    public FDProfiler(ExecutorService executor) {
        super(executor);
    }

    @Override
    public FDResult profile(FDRequest input) throws InputIterationException {
        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileCCWithSampling(lhs.relations(), rhs.relations(), lhs.level());
        }

        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            return profileCCLocked(lhs.relations(), rhs.attributes(), lhs.level());
        }

        if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.CC rhs){
            return profileLockedCC(lhs.attributes(), rhs.relations());
        }

        if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            return profileLocked(lhs.attributes(), rhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported FDRequest");
    }

    private Map<BitSet, List<BitSet>> profile(int relationIndex, int level) {
        try {
            Sampler2 sampler = this.preprocessor.getSampler2(relationIndex);
            ValidatorNew validator = this.preprocessor.getValidator(relationIndex);

            Set<IntIntImmutablePair> suggestions = new HashSet<>();
            do {
                NegativeCover newNonFds = sampler.run(suggestions);
                suggestions = validator.validateWithPositiveCover(this.executor, newNonFds, level);
            } while (suggestions != null);

            return validator.collectResults(level);
        }catch (ExecutionException | InterruptedException e){
            throw  new RuntimeException("Issue Occurred when profiling Relation: "+ relationIndex + "at level: " + level, e);
        }
    }

    private FDResult profileCCWithSampling(int[] lhsRelationIndexes, int[] rhsRelationIndexes, int level) {
        FDResult result = new FDResult();
        int[] commonRelationIndexes = Utility.intersect(lhsRelationIndexes, rhsRelationIndexes);

        for (int relationIndex : commonRelationIndexes) {
            Map<BitSet, List<BitSet>> results = profile(relationIndex, level);

            for (Map.Entry<BitSet, List<BitSet>> entry : results.entrySet()) {
                AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, entry.getKey());

                for (BitSet lhs : entry.getValue()) {
                    AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, lhs);
                    result.add(lhsAbs, rhsAbs);
                }
            }
        }

        return result;
    }

    private FDResult profileCCWithValidation(int[] lhsRelationIndexes, int[] rhsRelationIndexes, int level) {
        FDResult result = new FDResult();
        int[] commonRelationIndexes = Utility.intersect(lhsRelationIndexes, rhsRelationIndexes);

        for (int relationIndex : commonRelationIndexes) {
            try {
                ValidatorNew validator = this.preprocessor.getValidator(relationIndex);

                Map<BitSet, List<BitSet>> results = validator.validateDirectly(this.executor, level);

                for (Map.Entry<BitSet, List<BitSet>> entry : results.entrySet()) {
                    AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, entry.getKey());

                    for (BitSet lhs : entry.getValue()) {
                        AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, lhs);
                        result.add(lhsAbs, rhsAbs);
                    }
                }
            }catch (ExecutionException | InterruptedException e){
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    //================================================= OLD
    /*private Map<Integer, List<BitSet>> profile(int relationIndex, int level) {

        Sampler2 sampler = this.preprocessor.getSampler2(relationIndex);
        Validator validator = this.preprocessor.getValidator(relationIndex);

        List<IntIntImmutablePair> suggestions = new ArrayList<>();
        do {
            FDSet newNonFds = sampler.run(suggestions);
            suggestions = validator.validate(newNonFds, level);
        } while (suggestions != null);

        return validator.results(level);
    }

    private FDResult profileCCWithSampling2(int[] lhsRelationIndexes, int[] rhsRelationIndexes, int level) {
        FDResult result = new FDResult();
        int[] commonRelationIndexes = Utility.intersect(lhsRelationIndexes, rhsRelationIndexes);

        for (int relationIndex : commonRelationIndexes) {
            Map<Integer, List<BitSet>> results = profile(relationIndex, level);

            for(Map.Entry<Integer, List<BitSet>> entry : results.entrySet()){
                AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, entry.getKey());
                for (BitSet lhs : entry.getValue()) {
                    AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, lhs);
                    result.add(lhsAbs, rhsAbs);
                }
            }
        }

        return result;
    }*/

    private FDResult profileCC(int[] lhsRelationIndexes, int[] rhsRelationIndexes, int level) throws InputIterationException {
        FDResult result = new FDResult();
        int[] commonRelationIndexes = Utility.intersect(lhsRelationIndexes, rhsRelationIndexes);
        for(int relationIndex : commonRelationIndexes) {
            Utility.printLog(String.format("P: FD  R:%d L:%d", relationIndex, level), this.executor);

            ObjectOpenHashSet<FDResult.FD> foundFDs = new ObjectOpenHashSet<>(); //this.fdPerRelation.computeIfAbsent(relationIndex, k -> new ObjectOpenHashSet<>());

            AttributeBitSet[] currentLevel = this.preprocessor.generateApriori(relationIndex, level);
            AttributeBitSet[] initialLevel = this.preprocessor.generateApriori(relationIndex, 1);

            for (AttributeBitSet lhsAbs : currentLevel) {
                PositionListIndex lhsPli = this.preprocessor.getPLI(lhsAbs);

                for (AttributeBitSet rhsAbs : initialLevel) {
                    if(rhsAbs.isSubsetOf(lhsAbs)){ //for triviality pruning
                        continue;
                    }

                    PositionListIndex rhsPli = this.preprocessor.getPLI(rhsAbs);

                    if (isContainSubsetOf(foundFDs, lhsAbs, rhsAbs)) {
                        continue;
                    }

                    if(isFD(lhsPli, rhsPli)){
                        foundFDs.add(new FDResult.FD(lhsPli.getAttributeSet(), rhsPli.getAttributeSet()));
                        result.add(lhsPli.getAttributeSet(), rhsPli.getAttributeSet());
                    }
                }
            }
        }

        return  result;
    }

    private FDResult profileLockedCC(ObjectOpenHashSet<AttributeBitSet> lhsAttributes,
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

    private FDResult profileCCLocked(int[] lhsRelationIndexes,
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

    private FDResult profileLocked(ObjectOpenHashSet<AttributeBitSet> lhsAttributes,
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
        PositionListIndex intersectedPli = this.preprocessor.getPLI(abs);

        //If the rhsPli does not split any partitions of the lhsPli, the FD is valid!
        return lhsPli.getClusters().equals(intersectedPli.getClusters());
    }

    private boolean isContainSubsetOf(ObjectOpenHashSet<FDResult.FD> absSet, AttributeBitSet lhsAbs, AttributeBitSet rhsAbs){
        for(FDResult.FD fd : absSet){
            if(fd.rhs.equals(rhsAbs) && fd.lhs.isSubsetOf(lhsAbs)){
                return true;
            }
        }
        return false;
    }

    private String toAttrNames(BitSet indices, String[] headers) {
        if (indices.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        indices.stream().forEach(i -> sb.append(headers[i]).append(","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
}

