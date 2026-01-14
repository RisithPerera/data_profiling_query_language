package de.metathesis.profilers;


import de.metanome.algorithm_integration.input.InputIterationException;
import de.metathesis.Utility;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.requests.INDRequest;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.INDResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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

        if(input.lhs() instanceof SearchSpace.CC lhs && input.rhs() instanceof SearchSpace.CC rhs) {
            return profileCC(lhs.relations(), rhs.relations(), lhs.level());
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

        throw new IllegalArgumentException("Unsupported IDRequest");
    }

    private INDResult profileCC(int[] lhsRelationIndexes, int[] rhsRelationIndexes, int level) throws InputIterationException {
        INDResult result = new INDResult();

        return  result;
    }

    private INDResult profileLockedCC(ObjectOpenHashSet<AttributeBitSet> lhsAttributes,
                                      int[] rhsRelationIndexes) throws InputIterationException {
        INDResult result = new INDResult();

        return  result;
    }

    private INDResult profileCCLocked(int[] lhsRelationIndexes,
                                      ObjectOpenHashSet<AttributeBitSet> rhsAttributes,
                                      int level) throws InputIterationException {
        INDResult result = new INDResult();
        for(int relationIndex : lhsRelationIndexes){
            Utility.printLog(String.format("P: IND R:%d L:%d", relationIndex,  level), this.executor);
            String[][] lhsRecords = preprocessor.getColumnWiseDataOf(relationIndex);
            AttributeBitSet[] lhsAttributeSets = this.preprocessor.generateApriori(relationIndex, level);

            //Instructor.printLog("IND", this.executor);
            for(AttributeBitSet lhsAttributeSet : lhsAttributeSets){
                String[] lhsTuples = getTuples(lhsRecords, lhsAttributeSet);
                for(AttributeBitSet rhsAttributeSet : rhsAttributes){
                    if(lhsAttributeSet.getRelationIndex() == rhsAttributeSet.getRelationIndex() &&
                            !lhsAttributeSet.intersect(rhsAttributeSet).isEmpty()){
                        continue;
                    }

                    String[][] rhsRecords = preprocessor.getColumnWiseDataOf(rhsAttributeSet.getRelationIndex());
                    String[] rhsTuples = getTuples(rhsRecords, rhsAttributeSet);

                    if (isIncluded(lhsTuples, rhsTuples)) {
                        result.add(lhsAttributeSet, rhsAttributeSet);
                    }
                }
            }
        }
        return  result;
    }

    private INDResult profileLocked(ObjectOpenHashSet<AttributeBitSet> lhsAttributes,
                                    ObjectOpenHashSet<AttributeBitSet> rhsAttributes) throws InputIterationException {
        INDResult result = new INDResult();

        for(AttributeBitSet lhsAttributeSet : lhsAttributes){
            Utility.printLog(String.format("P: IND R:%d L:%d", lhsAttributeSet.getRelationIndex(),  lhsAttributeSet.size()), this.executor);
            String[][] lhsRecords = preprocessor.getColumnWiseDataOf(lhsAttributeSet.getRelationIndex());
            String[] lhsTuples = getTuples(lhsRecords, lhsAttributeSet);

            for(AttributeBitSet rhsAttributeSet : rhsAttributes){
                if(lhsAttributeSet.getRelationIndex() == rhsAttributeSet.getRelationIndex() &&
                        !lhsAttributeSet.intersect(rhsAttributeSet).isEmpty()){
                    continue;
                }

                String[][] rhsRecords = preprocessor.getColumnWiseDataOf(rhsAttributeSet.getRelationIndex());
                String[] rhsTuples = getTuples(rhsRecords, rhsAttributeSet);

                if (isIncluded(lhsTuples, rhsTuples)) {
                    result.add(lhsAttributeSet, rhsAttributeSet);
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

    private String[] buildTuples(String[][] columns, BitSet attrs) {
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

    private boolean isIncluded(String[] lhs, String[] rhs) {
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
