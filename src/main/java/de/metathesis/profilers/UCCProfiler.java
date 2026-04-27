package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Sampler;
import de.metathesis.UCCValidator;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.NegativeCover;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class UCCProfiler extends AbstractProfiler<UCCRequest, UCCResult> {

    public UCCProfiler(ExecutorService executor) {
        super(executor);
    }

    @Override
    public UCCResult profile(UCCRequest input) throws InputIterationException {

        if (input.lhs() instanceof SearchSpace.Free free) {
            return profileFree(free.relations(), free.level());
        }

        if (input.lhs() instanceof SearchSpace.Lock lhs) {
            return profileLock(lhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported UCCRequest");
    }

    private UCCResult profileFree(int[] lhsRelationIndexes, int level) {
        UCCResult result = new UCCResult();

        for (int relationIndex : lhsRelationIndexes) {
            try {
                Sampler sampler = this.preprocessor.getSampler(relationIndex);
                UCCValidator validator = this.preprocessor.getUCCValidator(relationIndex);

                Set<BitSet> validatedUCCSet = new ObjectOpenHashSet <>();
                Set<IntIntImmutablePair> suggestions = new ObjectOpenHashSet <>();
                do {
                    NegativeCover newNonFds = validator.isInitialValidation() && !sampler.isInitialSampling() ? sampler.getNegCover() : sampler.run(suggestions);
                    suggestions = validator.validateWithPositiveCover(this.executor, newNonFds, level, validatedUCCSet);
                } while (suggestions != null);


                for (BitSet ucc : validatedUCCSet) {
                    AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, ucc);
                    result.add(lhsAbs);
                }
            }catch (ExecutionException | InterruptedException e){
                throw  new RuntimeException("Issue Occurred when profiling Relation: "+ relationIndex + "at level: " + level, e);
            }
        }

        return result;
    }

    private UCCResult profileLock(ObjectOpenHashSet<AttributeBitSet> lhsAttributes) {
        UCCResult result = new UCCResult();

        Map<Integer, Set<BitSet>> lhsByRelation = new HashMap<>();
        for (AttributeBitSet lhsAbs : lhsAttributes) {
            int relIdx = lhsAbs.getRelationIndex();
            lhsByRelation.computeIfAbsent(relIdx, k -> new ObjectOpenHashSet<>()).add(lhsAbs.getAttributeIndexSet());
        }

        // Process each relation independently
        for (Map.Entry<Integer, Set<BitSet>> entry : lhsByRelation.entrySet()) {
            int relationIndex = entry.getKey();
            Set<BitSet> pendingList = entry.getValue();

            UCCValidator validator = this.preprocessor.getUCCValidator(relationIndex);
            Sampler sampler = this.preprocessor.getSampler(relationIndex);

            // Track confirmed valid rhs per lhs across rounds
            Set<BitSet> confirmedList = new ObjectOpenHashSet<>();

            Set<IntIntImmutablePair> suggestions = new HashSet<>();

            do {
                NegativeCover newNonFds = validator.isInitialValidation() && !sampler.isInitialSampling() ? sampler.getNegCover() : sampler.run(suggestions);
                suggestions = validator.validateLockedCandidates(newNonFds, pendingList, confirmedList);
            } while (suggestions != null);

            // Collect results
            for (BitSet ucc : confirmedList) {
                AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, ucc);
                result.add(lhsAbs);
            }
        }

        return result;
    }
}