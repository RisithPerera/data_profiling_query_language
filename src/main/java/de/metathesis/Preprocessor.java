package de.metathesis;

import de.metanome.MetanomeHelper;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.PositionListIndex;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.UCCResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;


public final class Preprocessor {

    private static final Preprocessor INSTANCE = new Preprocessor();

    private final Object2IntMap<String> relationToIndex = new Object2IntOpenHashMap<>();

    private final Int2ObjectMap<String> relationNames = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<String[]> attributeNames = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<String[][]> relationValues = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<RelationalInput> relationalInputs = new Int2ObjectOpenHashMap<>();

    //This structure hold entire calculated PLIs so far.
    private final Long2ObjectMap<Object2ObjectMap<AttributeBitSet, PositionListIndex>> pliMap = new Long2ObjectOpenHashMap<>();

    private final int inputRowLimit;
    private final boolean isNullEqualNull;

    private Preprocessor() {
        this.inputRowLimit = InputConfigurationSingleton.get().getFILE_MAX_ROWS();
        this.isNullEqualNull = InputConfigurationSingleton.get().getFILE_NULL_EQUALS_NULL();
        this.relationToIndex.defaultReturnValue(-1);
    }

    public static Preprocessor getInstance() {
        return INSTANCE;
    }

    public Map<String, int[]> initializeSearchSpace(Map<String, List<String>> relationMap) throws AlgorithmConfigurationException, InputGenerationException {
        // 1. Build relation universe
        for (List<String> relations : relationMap.values()) {
            for (String relation : relations) {
                if (relationToIndex.getInt(relation) == -1) {
                    System.out.println("Initialize Relation: " + relation);
                    int id = relationNames.size();
                    relationToIndex.put(relation, id);
                    relationNames.put(id, relation);

                    RelationalInputGenerator inputGenerator = MetanomeHelper.getInput(relation);
                    RelationalInput relationalInput = inputGenerator.generateNewCopy();
                    assert relationalInput != null : "Input generation failed!";

                    relationalInputs.put(id, relationalInput);

                    String[] columns = relationalInput.columnNames().toArray(new String[0]);
                    attributeNames.put(id, columns);
                }
            }
        }

        // 2. Initialize PLI map with level 0
        for(int relationIndex : relationToIndex.values()){
            long key = Utility.compositeKey(relationIndex, 0); //Level 0: Size 0
            AttributeBitSet abs = new AttributeBitSet(relationIndex, new BitSet());
            PositionListIndex pli = new PositionListIndex(abs, new ArrayList<>());

            Object2ObjectMap<AttributeBitSet, PositionListIndex> level0 = new Object2ObjectOpenHashMap<>();
            level0.put(abs, pli);

            this.pliMap.put(key, level0);
        }

        // 3. Build index map for variables (X, Y, Z)
        Map<String, int[]> relationIndexMap = new HashMap<>(relationMap.size());

        for (Map.Entry<String, List<String>> relationsEntry : relationMap.entrySet()) {
            List<String> relations = relationsEntry.getValue();
            int[] idx = new int[relations.size()];

            for (int i = 0; i < relations.size(); i++) {
                idx[i] = relationToIndex.getInt(relations.get(i));
            }

            relationIndexMap.put(relationsEntry.getKey(), idx);
        }

        return relationIndexMap;
    }

    public synchronized int getRelationIndexOf(String fileName) {
        return relationToIndex.getInt(fileName);
    }

    public synchronized int getAttributeSizeOf(int relationIndex) {
        return attributeNames.get(relationIndex).length;
    }

    public synchronized int[] getAttributeSizesOf(int[] relationIndexes) {
        int[] sizes = new int[relationIndexes.length];

        for (int i = 0; i < relationIndexes.length; i++) {
            sizes[i] = attributeNames.get(relationIndexes[i]).length;
        }

        return sizes;
    }

    public synchronized Map<String, int[]> getAttributeSizesOf(Map<String, int[]> relationIndexesMap) {
        Map<String, int[]> relationSizesMap = new HashMap<>(relationIndexesMap.size());
        for (Map.Entry<String, int[]> relationsEntry : relationIndexesMap.entrySet()) {
            relationSizesMap.put(relationsEntry.getKey(), getAttributeSizesOf(relationsEntry.getValue()));
        }
        return relationSizesMap;
    }

    public AttributeBitSet[] generateApriori(int relationIndex, int level) {
        int cols = getAttributeSizeOf(relationIndex);

        if (level < 0 ||  level > cols) {
            throw new IllegalArgumentException("Level " + level + " must be between column boundary (0:" + cols + ") of relation:" + relationIndex);
        }

        if(level == 0) {
            return new AttributeBitSet[]{new AttributeBitSet(relationIndex, new BitSet())};
        }

        int count = Utility.binomial(cols, level);
        AttributeBitSet[] result = new AttributeBitSet[count];

        BigInteger mask = BigInteger.ONE.shiftLeft(level).subtract(BigInteger.ONE);  // first combination
        BigInteger limit = BigInteger.ONE.shiftLeft(cols);

        int idx = 0;
        while (mask.compareTo(limit) < 0) {
            BitSet bitSet = toBitSet(mask, cols);
            result[idx++] = new AttributeBitSet(relationIndex, bitSet);

            // Gosper's hack for BigInteger
            BigInteger c = mask.and(mask.negate());
            BigInteger r = mask.add(c);
            mask = r.or(r.xor(mask).shiftRight(2).divide(c));
        }

        return result;
    }

    public synchronized String[][] getColumnWiseDataOf(int relationIndex) throws InputIterationException {
        if(relationValues.containsKey(relationIndex)) {
            return relationValues.get(relationIndex);
        }

        int numAttributes = attributeNames.get(relationIndex).length;

        RelationalInput relationalInput = relationalInputs.get(relationIndex);
        assert relationalInput != null : "Preprocessor Initialization Failed";

        int numRecords = 0;

        // temporary column-wise buffers
        ObjectArrayList<String>[] cols = new ObjectArrayList[numAttributes];
        for (int c = 0; c < numAttributes; c++) {
            cols[c] = new ObjectArrayList<>();
        }

        while (relationalInput.hasNext() && (this.inputRowLimit <= 0 || this.inputRowLimit != numRecords)) {
            List<String> record = relationalInput.next();

            for (int c = 0; c < numAttributes; c++) {
                cols[c].add(record.get(c));
            }

            numRecords++;
            if (numRecords > Integer.MAX_VALUE - 1) {
                throw new IllegalStateException("Number of records " + numRecords
                        + "exceeds max int for IntArrayList. Use long-based PLIs instead");
            }
        }

        // materialize final column arrays
        String[][] relationData = new String[numAttributes][];
        for (int c = 0; c < numAttributes; c++) {
            relationData[c] = cols[c].toArray(new String[0]);
        }

        relationValues.put(relationIndex, relationData);
        return relationData;
    }

    // Simulates reading a CSV and returning a Relation
    public synchronized PositionListIndex[] getInitialPLIs(int relationIndex) throws InputIterationException {
        long key = Utility.compositeKey(relationIndex, 1); //Level 1: Size 1

        if(this.pliMap.containsKey(key)) {
            return this.pliMap.get(key).values().toArray(new PositionListIndex[0]);
        }

        String[][] columnData = getColumnWiseDataOf(relationIndex);
        Object2ObjectMap<AttributeBitSet, PositionListIndex> pliArray = new Object2ObjectOpenHashMap<>();

        for (int c = 0; c < columnData.length; c++) {
            PositionListIndex pli = calculateInitialPLI(relationIndex, c, columnData[c]);
            pliArray.put(pli.getAttributeSet(), pli);
        }

        this.pliMap.put(key, pliArray);
        return pliArray.values().toArray(new PositionListIndex[0]);
    }

    public synchronized void addPLI(PositionListIndex pli){
        int relationIndex = pli.getAttributeSet().getRelationIndex();
        int level = pli.getAttributeSet().size();
        long key = Utility.compositeKey(relationIndex, level);

        this.pliMap.computeIfAbsent(key, k -> new Object2ObjectOpenHashMap<>()).put(pli.getAttributeSet(), pli);
    }

    public synchronized PositionListIndex getPLI(AttributeBitSet abs) throws InputIterationException {
        int relationIndex = abs.getRelationIndex();
        int level = abs.size();
        long key = Utility.compositeKey(relationIndex, level);

        Object2ObjectMap<AttributeBitSet, PositionListIndex> levelMap = this.pliMap.get(key);

        if (levelMap == null) {
            if(abs.size() == 1){
                int pos = abs.getAttributeIndexSet().nextSetBit(0);
                String[][] columnData = getColumnWiseDataOf(relationIndex);
                levelMap = new Object2ObjectOpenHashMap<>();
                PositionListIndex pli = calculateInitialPLI(relationIndex, pos, columnData[pos]);
                levelMap.put(pli.getAttributeSet(), pli);
                this.pliMap.put(key, levelMap);
                return pli;
            }else{
                long level1Key = Utility.compositeKey(relationIndex, 1);
                Object2ObjectMap<AttributeBitSet, PositionListIndex> preLevelMap = this.pliMap.get(level1Key);
                PositionListIndex pliSingle = null;
                for(int index : abs.getAttributeIndexSet().stream().toArray()){
                    AttributeBitSet absSingle = new AttributeBitSet(relationIndex, index);
                    if(pliSingle == null){
                        pliSingle = preLevelMap.get(absSingle);
                    }else{
                        pliSingle = pliSingle.intersect(preLevelMap.get(absSingle));
                    }
                }

                return pliSingle;
            }
        }

        PositionListIndex pli = levelMap.get(abs);

        if (pli == null) {
            if(abs.size() == 1){
                int pos = abs.getAttributeIndexSet().nextSetBit(0);
                String[][] columnData = getColumnWiseDataOf(relationIndex);
                pli = calculateInitialPLI(relationIndex, pos, columnData[pos]);
                levelMap.put(pli.getAttributeSet(), pli);
                return pli;
            }else{
                long level1Key = Utility.compositeKey(relationIndex, 1);
                Object2ObjectMap<AttributeBitSet, PositionListIndex> preLevelMap = this.pliMap.get(level1Key);
                PositionListIndex pliSingle = null;
                for(int index : abs.getAttributeIndexSet().stream().toArray()){
                    AttributeBitSet absSingle = new AttributeBitSet(relationIndex, index);
                    if(pliSingle == null){
                        pliSingle = preLevelMap.get(absSingle);
                    }else{
                        pliSingle = pliSingle.intersect(preLevelMap.get(absSingle));
                    }
                }

                return pliSingle;
            }
        }

        return pli;
    }

    public PositionListIndex getOrComputePLI(AttributeBitSet abs, Supplier<PositionListIndex> computer) {

        int relationIndex = abs.getRelationIndex();
        int level = abs.size();
        long key = Utility.compositeKey(relationIndex, level);

        synchronized (this) {
            var levelMap = this.pliMap.computeIfAbsent(key, k -> new Object2ObjectOpenHashMap<>());

            return levelMap.computeIfAbsent(abs, a -> computer.get());
        }
    }

    private PositionListIndex calculateInitialPLI(int relationIndex, int columnIndex, String[] column){
        Map<String, IntArrayList> clusterMap = new HashMap<>();

        int rowIndex = 0;
        for(String value : column) {
            if (clusterMap.containsKey(value)) {
                clusterMap.get(value).add(rowIndex);
            } else {
                IntArrayList newCluster = new IntArrayList();
                newCluster.add(rowIndex);
                clusterMap.put(value, newCluster);
            }
            rowIndex++;
        }

        List<IntArrayList> clusters = new ArrayList<>();

        if (!this.isNullEqualNull) {
            clusterMap.remove(null);
        }

        for (IntArrayList cluster : clusterMap.values()) {
            if (cluster.size() > 1)
                clusters.add(cluster);
        }

        AttributeBitSet attributeBitSet = new AttributeBitSet(relationIndex, columnIndex);
        return new PositionListIndex(attributeBitSet, clusters);
    }

    private String format(AttributeBitSet abs) {

        String relName = relationNames.get(abs.getRelationIndex());
        String[] cols = attributeNames.get(abs.getRelationIndex());

        StringBuilder sb = new StringBuilder();
        sb.append(relName).append("(");

        BitSet bs = abs.getAttributeIndexSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(cols[i]).append(';');
        }
        sb.setLength(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    public void printAttributeSet(AttributeBitSet... sets) {
        for (int i = 0; i < sets.length; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(format(sets[i]));
        }
        System.out.println();
    }

    public void printUCC(UCCResult result) {
        result.forEach(ucc -> System.out.println(format(ucc.lhs)));
    }

    public void printIND(INDResult result) {
        result.forEach(ind -> System.out.println(format(ind.lhs) + " ⊆ " + format(ind.rhs)));
    }

    public void printFD(FDResult result) {
        result.forEach(fd -> System.out.println(format(fd.lhs) + " → " + format(fd.rhs)));
    }

    // Convert BigInteger mask to BitSet
    private static BitSet toBitSet(BigInteger mask, int cols) {
        BitSet bs = new BitSet(cols);
        for (int i = 0; i < cols; i++) {
            if (mask.testBit(i)) bs.set(i);
        }
        return bs;
    }
}