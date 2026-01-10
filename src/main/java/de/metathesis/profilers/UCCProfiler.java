package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Utility;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.concurrent.Executor;

public class UCCProfiler extends AbstractProfiler<UCCRequest, UCCResult> {

    private final Long2ObjectMap<ObjectArrayList<PositionListIndex>> nonUniquePLIs = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ObjectOpenHashSet<AttributeBitSet>> foundUCCs = new Long2ObjectOpenHashMap<>();

    public UCCProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public UCCResult profile(UCCRequest request) throws InputIterationException {
        UCCResult result = new UCCResult();

        if(request.lhs() instanceof SearchSpace.CC cc){
            for(int relationIndex : cc.relations()){
                Utility.printLog(String.format("P: UCC R:%d L:%d", relationIndex, cc.level()), this.executor);
                long currentKey = key(relationIndex, cc.level());
                long previousKey = key(relationIndex, cc.level() - 1);
                if(cc.level() == 1){
                    PositionListIndex[] plis = this.preprocessor.getPositionListIndexesOf(relationIndex);
                    ObjectArrayList<PositionListIndex> currentNonUniques = new ObjectArrayList<>();

                    // Calculate all unary UCCs and unary non-UCCs
                    for(PositionListIndex pli : plis) {
                        if (pli.isUnique()) {
                            result.add(pli.getAttributeSet());
                        } else {
                            currentNonUniques.add(pli);
                        }
                    }
                    nonUniquePLIs.put(currentKey, currentNonUniques);
                }else{
                    ObjectArrayList<PositionListIndex> previousNonUCCs = this.nonUniquePLIs.get(previousKey);
                    ObjectOpenHashSet<AttributeBitSet> foundUCCs = this.foundUCCs.computeIfAbsent(relationIndex, k -> new ObjectOpenHashSet<>());

                    if(previousNonUCCs == null){
                        return result;
                    }

                    ObjectArrayList<PositionListIndex> currentNonUniques = new ObjectArrayList<>();
                    ObjectOpenHashSet<AttributeBitSet> calculatedAttributeSet = new ObjectOpenHashSet<>();

                    for (int i = 0; i < previousNonUCCs.size(); i++) {
                        for (int j = i + 1; j < previousNonUCCs.size(); j++) {
                            PositionListIndex pli1 = previousNonUCCs.get(i);
                            PositionListIndex pli2 = previousNonUCCs.get(j);

                            AttributeBitSet abs = pli1.getAttributeSet().union(pli2.getAttributeSet());
                            if(isContainUniqueSubset(abs, foundUCCs)) continue;

                            // Calculate all unary UCCs and unary non-UCCs
                            PositionListIndex pli = pli1.intersect(pli2);
                            if(!calculatedAttributeSet.contains(pli.getAttributeSet()) && pli.getAttributeSet().size() == cc.level()){
                                calculatedAttributeSet.add(pli.getAttributeSet());
                                if (pli.isUnique()) {
                                    foundUCCs.add(pli.getAttributeSet());
                                    result.add(pli.getAttributeSet());
                                } else {
                                    currentNonUniques.add(pli);
                                }
                            }
                        }
                    }
                    nonUniquePLIs.put(currentKey, currentNonUniques);
                }
            }
        }

        return result;
    }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    private boolean isContainUniqueSubset(AttributeBitSet abs, ObjectOpenHashSet<AttributeBitSet> previousUCCs){
        for(AttributeBitSet ucc : previousUCCs){
            if(ucc.isSubsetOf(abs)){
                return true;
            }
        }
        return false;
    }
}