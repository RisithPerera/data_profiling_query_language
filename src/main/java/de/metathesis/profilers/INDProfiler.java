package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Instructor;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.INDResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.Executor;

public class INDProfiler extends AbstractProfiler<INDRequest, INDResult> {

    private final Object2ObjectOpenHashMap<AttributeBitSet, String[]> cashedTuples = new Object2ObjectOpenHashMap<>();

    public INDProfiler(Executor executor) {
        super(executor);
    }

    @Override
    public INDResult profile(INDRequest input) throws InputIterationException {
        INDResult result = new INDResult();

        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.Locked rhs) {
            for(int relationIndex : lhs.relations()){
                Instructor.printLog(String.format("P: IND R:%d L:%d", relationIndex,  lhs.level()), this.executor);
                String[][] lhsRecords = preprocessor.getColumnWiseDataOf(relationIndex);
                AttributeBitSet[] lhsAttributeSets = this.preprocessor.generateApriori(relationIndex, lhs.level());

                //Instructor.printLog("IND", this.executor);
                for(AttributeBitSet lhsAttributeSet : lhsAttributeSets){
                    String[] lhsTuples = getTuples(lhsRecords, lhsAttributeSet);
                    for(AttributeBitSet rhsAttributeSet : rhs.attributes()){
                        if(lhsAttributeSet.getRelationIndex() == rhsAttributeSet.getRelationIndex()
                                && lhsAttributeSet.intersect(rhsAttributeSet).size() != 0) continue;

                        String[][] rhsRecords = preprocessor.getColumnWiseDataOf(rhsAttributeSet.getRelationIndex());
                        String[] rhsTuples = getTuples(rhsRecords, rhsAttributeSet);

                        if (isIncluded(lhsTuples, rhsTuples)) {
                            result.add(lhsAttributeSet, rhsAttributeSet);
                        }
                    }
                }
            }
        }

        return result;
    }

    private String[] getTuples(String[][] lhsRecords, AttributeBitSet attributeBitSet){
        if(cashedTuples.containsKey(attributeBitSet)){
            return cashedTuples.get(attributeBitSet);
        }

        return buildTuples(lhsRecords, attributeBitSet.getAttributeIndexSet());
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
