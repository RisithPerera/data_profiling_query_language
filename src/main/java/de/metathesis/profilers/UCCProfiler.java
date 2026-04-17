package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.FDValidator;
import de.metathesis.Sampler;
import de.metathesis.UCCValidator;
import de.metathesis.utils.Utility;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.NegativeCover;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class UCCProfiler extends AbstractProfiler<UCCRequest, UCCResult> {

    private final Long2ObjectMap<ObjectArrayList<AttributeBitSet>> nonUCCPerRelationLevel = new Long2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ObjectOpenHashSet<AttributeBitSet>> uccPerRelation = new Int2ObjectOpenHashMap<>();

    public UCCProfiler(ExecutorService executor) {
        super(executor);
    }

    @Override
    public UCCResult profile(UCCRequest input) throws InputIterationException {

        if (input.lhs() instanceof SearchSpace.CC cc) {
            return profileCCWithSampling(cc.relations(), cc.level());
        }

        if (input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs) {
            return profileLocked(lhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported UCCRequest");
    }

    private Set<BitSet> profile(int relationIndex, int level) {
        try {
            Sampler sampler = this.preprocessor.getSampler(relationIndex);
            UCCValidator validator = this.preprocessor.getUCCValidator(relationIndex);

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

    private UCCResult profileCCWithSampling(int[] lhsRelationIndexes, int level) {
        UCCResult result = new UCCResult();

        for (int relationIndex : lhsRelationIndexes) {
            Set<BitSet> results = profile(relationIndex, level);

            for (BitSet ucc : results) {
                AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, ucc);
                result.add(lhsAbs);
            }
        }

        return result;
    }

    //------------------------------------- OLD Code -------------------------//
    private UCCResult profileCC(int[] lhsRelations, int level) throws InputIterationException {
        UCCResult result = new UCCResult();
        for(int relationIndex : lhsRelations) {
            Utility.printLog(String.format("P: UCC R:%d L:%d", relationIndex, level), this.executor);
            long currentKey = Utility.compositeKey(relationIndex, level);
            long previousKey = Utility.compositeKey(relationIndex, level - 1);

            ObjectArrayList<AttributeBitSet> currentNonUniques = new ObjectArrayList<>();
            ObjectOpenHashSet<AttributeBitSet> foundUCCs = this.uccPerRelation.computeIfAbsent(relationIndex, k -> new ObjectOpenHashSet<>());

            if (level == 1) {
                PositionListIndex[] plis = this.preprocessor.getRelation(relationIndex).getUnaryPLIs();

                // Calculate all unary UCCs and unary non-UCCs
                for (PositionListIndex pli : plis) {
                    if (pli.isUnique()) {
                        foundUCCs.add(pli.getAttributeSet());
                        result.add(pli.getAttributeSet());
                    } else {
                        currentNonUniques.add(pli.getAttributeSet());
                    }
                }

            } else {
                ObjectArrayList<AttributeBitSet> previousNonUCCs = this.nonUCCPerRelationLevel.get(previousKey);

                if (previousNonUCCs == null) {
                    return result;
                }


                ObjectOpenHashSet<AttributeBitSet> calculatedAttributeSet = new ObjectOpenHashSet<>();

                for (int i = 0; i < previousNonUCCs.size(); i++) {
                    PositionListIndex pli1 = this.preprocessor.getPLI(previousNonUCCs.get(i));
                    for (int j = i + 1; j < previousNonUCCs.size(); j++) {
                        PositionListIndex pli2 = this.preprocessor.getPLI(previousNonUCCs.get(j));

                        AttributeBitSet abs = pli1.getAttributeSet().union(pli2.getAttributeSet());
                        if (abs.size() != level) {
                            continue;
                        }

                        if (isContainSubsetOf(foundUCCs, abs)) {
                            continue;
                        }

                        //Get the cached intersected PLI or compute
                        PositionListIndex pli = this.preprocessor.getPLI(abs);

                        if (!calculatedAttributeSet.contains(pli.getAttributeSet()) && pli.getAttributeSet().size() == level) {
                            calculatedAttributeSet.add(pli.getAttributeSet());
                            if (pli.isUnique()) {
                                foundUCCs.add(pli.getAttributeSet());
                                result.add(pli.getAttributeSet());
                            } else {
                                currentNonUniques.add(pli.getAttributeSet());
                            }
                        }
                    }
                }
            }

            this.nonUCCPerRelationLevel.put(currentKey, currentNonUniques);
        }
            return result;
    }

    private UCCResult profileLocked(ObjectOpenHashSet<AttributeBitSet> lhsAttrs) throws InputIterationException {
        UCCResult result = new UCCResult();
        for(AttributeBitSet abs : lhsAttrs){
            PositionListIndex pli = this.preprocessor.getPLI(abs);
            if (pli.isUnique()) {
                result.add(pli.getAttributeSet());
            }
        }

        return result;
    }

    private boolean isContainSubsetOf(ObjectOpenHashSet<AttributeBitSet> absSet, AttributeBitSet abs){
        for(AttributeBitSet ucc : absSet){
            if(ucc.isSubsetOf(abs)){
                return true;
            }
        }
        return false;
    }
}