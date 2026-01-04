package de.metathesis.profilers;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Instructor;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class UCCProfiler extends AbstractProfiler<UCCRequest, UCCResult> {

    private final Long2ObjectMap<PositionListIndex[]> nonUniquePLIs = new Long2ObjectOpenHashMap<>();

    public UCCProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public UCCResult profile(UCCRequest request) throws InputIterationException {
        UCCResult result = new UCCResult();

        if(request.side() instanceof SearchSpace.CC cc){
            for(int relationIndex : cc.relations()){
                Instructor.printLog(String.format("P: UCC R:%d L:%d", relationIndex, cc.level()), this.executor);
                if(cc.level() == 1){
                    PositionListIndex[] plis = this.preprocessor.getPositionListIndexesOf(relationIndex);
                    List<PositionListIndex> currentNonUniques = new ArrayList<>();

                    // Calculate all unary UCCs and unary non-UCCs
                    for(PositionListIndex pli : plis) {
                        if (pli.isUnique()) {
                            result.add(pli.getAttributeSet());
                        } else {
                            currentNonUniques.add(pli);
                        }
                    }

                    nonUniquePLIs.put(key(relationIndex, cc.level()), currentNonUniques.toArray(new PositionListIndex[0]));
                }else{
                    PositionListIndex[] nonUniques = nonUniquePLIs.get(key(relationIndex, cc.level() - 1));
                    if(nonUniques == null){return result;}

                    List<PositionListIndex> currentNonUniques = new ArrayList<>();
                    ObjectOpenHashSet<AttributeBitSet> calculatedAttributeSet = new ObjectOpenHashSet<>();

                    for (int i = 0; i < nonUniques.length; i++) {
                        for (int j = i + 1; j < nonUniques.length; j++) {
                            PositionListIndex pli1 = nonUniques[i];
                            PositionListIndex pli2 = nonUniques[j];


                            // Calculate all unary UCCs and unary non-UCCs
                            PositionListIndex pli = pli1.intersect(pli2);
                            if(!calculatedAttributeSet.contains(pli.getAttributeSet()) && pli.getAttributeSet().size() == cc.level()){
                                calculatedAttributeSet.add(pli.getAttributeSet());
                                if (pli.isUnique()) {
                                    result.add(pli.getAttributeSet());
                                } else {
                                    currentNonUniques.add(pli);
                                }
                            }
                        }
                    }
                    nonUniquePLIs.put(key(relationIndex, cc.level()), currentNonUniques.toArray(new PositionListIndex[0]));
                }
            }
        }

        return result;
    }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    /*private void simpleWalk(Relation relation, List<UCCResult> uniques, List<PositionListIndex> currentLevel){

        if(currentLevel.isEmpty()){
            return;
        }

        int numAttributes = relation.getAttributes().length;

        for(int k = 0; k < numAttributes - 1; k++){
            Set<AttributeList> combinations = new HashSet<>();
            List<PositionListIndex> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i++) {
                for (int j = i + 1; j < currentLevel.size(); j++) {
                    PositionListIndex pli1 = currentLevel.get(i);
                    PositionListIndex pli2 = currentLevel.get(j);

                    AttributeList combination = pli1.getAttributeIndexList().union(pli2.getAttributeIndexList());
                    System.out.println(i + " " + j + " " + combination);

                    if(!combinations.contains(combination) && combination.size() == currentLevel.get(i).getAttributeIndexList().size() + 1){
                        combinations.add(combination);
                        PositionListIndex pli3 = pli1.intersect(pli2);
                        if (pli3.isUnique() && !isContainUniqueSubset(combination, uniques)) {
                            uniques.add(new UCC(relation, pli3.getAttributeIndexList()));
                        }else{
                            nextLevel.add(pli3); //Only add non unique CC for next level
                        }
                    }
                }
            }

            currentLevel = nextLevel;
            System.out.println("=========================");
        }
    }*/
}