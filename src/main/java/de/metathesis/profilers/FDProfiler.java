package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.FDValidator;
import de.metathesis.Sampler;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.NegativeCover;
import de.metathesis.structures.requests.FDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.FDResult;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class FDProfiler extends AbstractProfiler<FDRequest, FDResult> {

    public FDProfiler(ExecutorService executor) {
        super(executor);
    }

    @Override
    public FDResult profile(FDRequest input) throws InputIterationException {
        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileFreeFree(lhs.relations(), rhs.relations(), lhs.level());
        }

        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            return profileFreeLock(lhs.relations(), rhs.attributes());
        }

        if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.CC rhs){
            return profileLockFree(lhs.attributes(), rhs.relations());
        }

        if(input.lhs() instanceof SearchSpace.Locked lhs && input.rhs() instanceof SearchSpace.Locked rhs){
            return profileLockLock(lhs.attributes(), rhs.attributes());
        }

        throw new IllegalArgumentException("Unsupported FDRequest");
    }

    //Checked
    private FDResult profileFreeFree(int[] lhsRelationIndexes, int[] rhsRelationIndexes, int level) {
        FDResult result = new FDResult();
        int[] commonRelationIndexes = Utility.intersect(lhsRelationIndexes, rhsRelationIndexes);

        for (int relationIndex : commonRelationIndexes) {
            try {
                Sampler sampler = this.preprocessor.getSampler(relationIndex);
                FDValidator validator = this.preprocessor.getFDValidator(relationIndex);

                List<ObjectObjectImmutablePair<BitSet, BitSet>> results = new ArrayList<>();
                Set<IntIntImmutablePair> suggestions = new HashSet<>();
                do {
                    NegativeCover newNonFds = validator.isInitialValidation() && !sampler.isInitialSampling() ? sampler.getNegCover() : sampler.run(suggestions);
                    suggestions = validator.validateFreeFree(this.executor, newNonFds, level, results);
                } while (suggestions != null);

                for(ObjectObjectImmutablePair<BitSet, BitSet> pair : results){
                    AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, pair.left());

                    for (int attr = pair.right().nextSetBit(0); attr >= 0; attr = pair.right().nextSetBit(attr + 1)) {
                        AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, attr);
                        result.add(lhsAbs, rhsAbs);
                    }
                }
            }catch (ExecutionException | InterruptedException e){
                throw  new RuntimeException("Issue Occurred when profiling Relation: "+ relationIndex + "at level: " + level, e);
            }
        }

        return result;
    }

    //Checked
    private FDResult profileLockFree(ObjectOpenHashSet<AttributeBitSet> lhsAttributes, int[] rhsRelationIndexes) {
        FDResult result = new FDResult();

        if(lhsAttributes.isEmpty()) return result;

        // Group lhs by relationIndex, only if that relation is in rhsRelationIndexes
        IntOpenHashSet rhsRelationIndexSet = new IntOpenHashSet(rhsRelationIndexes);

        Map<Integer, Set<BitSet>> lhsMapByRelation = new HashMap<>();
        for (AttributeBitSet lhsAbs : lhsAttributes) {
            int lhsRelationIndex = lhsAbs.getRelationIndex();
            if (!rhsRelationIndexSet.contains(lhsRelationIndex)){
                continue;
            }
            lhsMapByRelation.computeIfAbsent(lhsRelationIndex, k -> new ObjectOpenHashSet<>()).add(lhsAbs.getAttributeIndexSet());
        }

        // Process each relation independently
        for (Map.Entry<Integer, Set<BitSet>> entry : lhsMapByRelation.entrySet()) {
            int relationIndex = entry.getKey();
            Set<BitSet> lhsList = entry.getValue();

            FDValidator validator = this.preprocessor.getFDValidator(relationIndex);
            Sampler sampler = this.preprocessor.getSampler(relationIndex);

            // Build pending pairs: lhs -> to all rhs remaining at once
            List<ObjectObjectImmutablePair<BitSet, BitSet>> pendingList = new ArrayList<>();
            for (BitSet lhs : lhsList) {
                BitSet rhs = new BitSet(validator.getNumAttributes());
                rhs.set(0, validator.getNumAttributes());
                rhs.andNot(lhs);
                pendingList.add(new ObjectObjectImmutablePair<>(lhs, rhs));
            }

            // Track confirmed valid rhs per lhs across rounds
            List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmedFDList = new ArrayList<>();

            Set<IntIntImmutablePair> suggestions = new HashSet<>();

            do {
                NegativeCover newNonFds = validator.isInitialValidation() && !sampler.isInitialSampling() ? sampler.getNegCover() : sampler.run(suggestions);
                suggestions = validator.validateLockFree(newNonFds, pendingList, confirmedFDList);
            } while (suggestions != null);

            // Collect results
            for (ObjectObjectImmutablePair<BitSet, BitSet> confirmedPair : confirmedFDList) {
                BitSet lhs = confirmedPair.left();
                BitSet rhs = confirmedPair.right();

                AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, lhs);
                for (int attr = rhs.nextSetBit(0); attr >= 0; attr = rhs.nextSetBit(attr + 1)) {
                    AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, attr);
                    result.add(lhsAbs, rhsAbs);
                }
            }
        }

        return result;
    }

    //Checked
    private FDResult profileFreeLock(int[] lhsRelationIndexes, ObjectOpenHashSet<AttributeBitSet> rhsAttributes) {
        FDResult result = new FDResult();

        if(rhsAttributes.isEmpty()) return result;

        // Group lhs by relationIndex, only if that relation is in rhsRelationIndexes
        IntOpenHashSet lhsRelationIndexSet = new IntOpenHashSet(lhsRelationIndexes);

        Map<Integer, Set<BitSet>> rhsMapByRelation = new HashMap<>();
        for (AttributeBitSet lhsAbs : rhsAttributes) {
            int lhsRelationIndex = lhsAbs.getRelationIndex();
            if (!lhsRelationIndexSet.contains(lhsRelationIndex)){
                continue;
            }
            rhsMapByRelation.computeIfAbsent(lhsRelationIndex, k -> new ObjectOpenHashSet<>()).add(lhsAbs.getAttributeIndexSet());
        }

        // Process each relation independently
        for (Map.Entry<Integer, Set<BitSet>> entry : rhsMapByRelation.entrySet()) {
            int relationIndex = entry.getKey();
            Set<BitSet> rhsCandidateList = entry.getValue();

            Sampler sampler = this.preprocessor.getSampler(relationIndex);
            FDValidator validator = this.preprocessor.getFDValidator(relationIndex);

            try {
                List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmedFDList = new ArrayList<>();
                Set<IntIntImmutablePair> suggestions = new HashSet<>();

                do {
                    NegativeCover newNonFds = validator.isInitialValidation() && !sampler.isInitialSampling() ? sampler.getNegCover() : sampler.run(suggestions);
                    suggestions = validator.validateFreeLock(newNonFds, rhsCandidateList, confirmedFDList);
                } while (suggestions != null);

                for (ObjectObjectImmutablePair<BitSet, BitSet> confirmedPair : confirmedFDList) {
                    AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, confirmedPair.left());
                    AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, confirmedPair.right());
                    result.add(lhsAbs, rhsAbs);
                }
            }catch (ExecutionException | InterruptedException e){
                throw  new RuntimeException("Issue Occurred when profiling Relation: "+ relationIndex + "at level: " + 0, e);
            }

        }

        return result;
    }

    //Not Check
    private FDResult profileLockLock(ObjectOpenHashSet<AttributeBitSet> lhsAttributes, ObjectOpenHashSet<AttributeBitSet> rhsAttributes) {
        FDResult result = new FDResult();

        if(lhsAttributes.isEmpty() || rhsAttributes.isEmpty()) return result;

        // Process each relation independently
        for (AttributeBitSet lhs : lhsAttributes) {
            int relationIndex = lhs.getRelationIndex();

            FDValidator validator = this.preprocessor.getFDValidator(relationIndex);
            Sampler sampler = this.preprocessor.getSampler(relationIndex);

            // Build pending pairs: lhs -> to all rhs remaining at once
            List<ObjectObjectImmutablePair<BitSet, BitSet>> pendingList = new ArrayList<>();
            for (AttributeBitSet rhs : rhsAttributes) {
                if(rhs.getRelationIndex() == relationIndex && !lhs.getAttributeIndexSet().intersects(rhs.getAttributeIndexSet())){
                    pendingList.add(new ObjectObjectImmutablePair<>(lhs.getAttributeIndexSet(), rhs.getAttributeIndexSet()));
                }
            }

            // Track confirmed valid rhs per lhs across rounds
            List<ObjectObjectImmutablePair<BitSet, BitSet>> confirmedFDList = new ArrayList<>();

            Set<IntIntImmutablePair> suggestions = new HashSet<>();

            do {
                NegativeCover newNonFds = validator.isInitialValidation() && !sampler.isInitialSampling() ? sampler.getNegCover() : sampler.run(suggestions);
                suggestions = validator.validateLockLock(newNonFds, pendingList, confirmedFDList);
            } while (suggestions != null);

            // Collect results
            for (ObjectObjectImmutablePair<BitSet, BitSet> confirmedPair : confirmedFDList) {
                AttributeBitSet lhsAbs = new AttributeBitSet(relationIndex, confirmedPair.left());
                AttributeBitSet rhsAbs = new AttributeBitSet(relationIndex, confirmedPair.right());

                result.add(lhsAbs, rhsAbs);
            }
        }

        return result;
    }
}

