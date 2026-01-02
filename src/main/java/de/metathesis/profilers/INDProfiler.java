package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.INDResult;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.Executor;

public class INDProfiler extends AbstractProfiler<INDRequest, INDResult> {

    public INDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public INDResult profile(INDRequest input) throws InputIterationException {
        INDResult result = new INDResult();

        if(input.lhs() instanceof SearchSpace.CC cc && input.rhs() instanceof SearchSpace.Locked locked) {
            for(int relationIndex : cc.relations()){
                System.out.println("Profiling  IND -> Relation: " + relationIndex + " Level: "+ cc.level());
                String[][] lhsRecords = preprocessor.getValueSetOf(relationIndex);
                int numRows = lhsRecords[0].length;
                AttributeBitSet[] lhsAttributeSets = this.preprocessor.generateApriori(relationIndex, cc.level());

                for(AttributeBitSet lhsAttributeSet : lhsAttributeSets){
                    String[] lhsTuples = buildTuples(lhsRecords, lhsAttributeSet.getAttributeIndexSet());
                    for(AttributeBitSet rhsAttributeSet : locked.attributes()){
                        if(!lhsAttributeSet.equals(rhsAttributeSet)){
                            String[][] rhsRecords = preprocessor.getValueSetOf(rhsAttributeSet.getRelationIndex());
                            String[] rhsTuples = buildTuples(rhsRecords, rhsAttributeSet.getAttributeIndexSet());

                            if (isIncluded(lhsTuples, rhsTuples)) {
                                result.add(lhsAttributeSet, rhsAttributeSet);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static String[] buildTuples(String[][] columns, BitSet attrs) {
        int numRows = columns[0].length;

        String[] tmp = new String[numRows];
        int[] cols = attrs.stream().toArray();

        for (int r = 0; r < numRows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c : cols) {
                sb.append(columns[c][r]).append('\u0001');
            }
            tmp[r] = sb.toString();
        }

        // sort
        Arrays.sort(tmp);

        // deduplicate
        int unique = 0;
        for (int i = 0; i < tmp.length; i++) {
            if (i == 0 || !tmp[i].equals(tmp[i - 1])) {
                tmp[unique++] = tmp[i];
            }
        }

        return Arrays.copyOf(tmp, unique);
    }

    private static boolean isIncluded(String[] lhs, String[] rhs) {
        int i = 0, j = 0;
        while (i < lhs.length && j < rhs.length) {
            int cmp = lhs[i].compareTo(rhs[j]);
            if (cmp == 0) { i++; j++; }
            else if (cmp > 0) j++;
            else return false;
        }
        return i == lhs.length;
    }

}
