package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.requests.UCCRequest;
import de.metathesis.structures.results.UCCResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class UCCProfiler extends AbstractProfiler<UCCRequest, List<UCCResult>> {

    public UCCProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public List<UCCResult> profile(UCCRequest request) throws InputIterationException {
        PositionListIndex[] plis = this.preprocessor.getPositionListIndexesOf(0);

        List<UCCResult> uniques = new ArrayList<>();
        List<PositionListIndex> currentNonUniques = new ArrayList<>();

        // Calculate all unary UCCs and unary non-UCCs
        for(PositionListIndex pli : plis) {
            if (pli.isUnique()) {
                uniques.add(new UCCResult(0, pli.getAttributeIndexList()));
            } else {
                currentNonUniques.add(pli);
            }
        }

        //simpleWalk(relation, uniques, currentNonUniques);

        return uniques;
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