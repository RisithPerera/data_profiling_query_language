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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.math.BigInteger;
import java.util.*;


public final class Preprocessor {

    private static final Preprocessor INSTANCE = new Preprocessor();

    private final Object2IntMap<String> relationToIndex = new Object2IntOpenHashMap<>();

    private final List<String> relationNames = new ArrayList<>();
    private final List<String[]> attributeNames = new ArrayList<>();
    private final List<String[][]> relationValues = new ArrayList<>();
    private final List<RelationalInput> relationalInputs = new ArrayList<>();
    private final Int2ObjectMap<PositionListIndex[]> relationPLIsMap = new Int2ObjectOpenHashMap<>();

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
                    relationNames.add(relation);

                    RelationalInputGenerator inputGenerator = MetanomeHelper.getInput(relation);
                    RelationalInput relationalInput = inputGenerator.generateNewCopy();
                    assert relationalInput != null : "Input generation failed!";

                    relationalInputs.add(relationalInput);

                    String[] columns = relationalInput.columnNames().toArray(new String[0]);
                    attributeNames.add(columns);
                }
            }
        }

        // 2. Build index map for variables (X, Y, Z)
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

    public synchronized String[][] getValueSetOf(int relationIndex){
        return relationValues.get(relationIndex);
    }

    public AttributeBitSet[] generateApriori(int relationIndex, int level) {
        int cols = getAttributeSizeOf(relationIndex);

        if (level < 1) {
            throw new IllegalArgumentException("Level must be between relation column boundary " + relationIndex + " " + level + " " + cols);
        }

        if (level > cols) {
            return new AttributeBitSet[0];
        }

        int count = binomial(cols, level);
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

    // Simulates reading a CSV and returning a Relation
    public synchronized PositionListIndex[] getPositionListIndexesOf(int relationIndex) throws InputIterationException {

        if(relationPLIsMap.containsKey(relationIndex)) {
            System.out.println("Return Cached PLI: " + relationIndex);
            return relationPLIsMap.get(relationIndex);
        }

        RelationalInput relationalInput = relationalInputs.get(relationIndex);
        assert relationalInput != null : "Initialization is important!";

        int numAttributes = attributeNames.get(relationIndex).length;

        Map<String, IntArrayList>[] clusters = calculateClusterMaps(relationalInput, numAttributes);
        List<PositionListIndex> plis = fetchPositionListIndexes(relationIndex, clusters);

        PositionListIndex[] plisArray = plis.toArray(new PositionListIndex[0]);

        relationPLIsMap.put(relationIndex, plisArray);
        return plisArray;
    }

    private Map<String, IntArrayList>[] calculateClusterMaps(RelationalInput relationalInput, int numAttributes) throws InputIterationException {
        Map<String, IntArrayList>[] clusterMaps = new HashMap[numAttributes];

        for (int i = 0; i < numAttributes; i++) {
            clusterMaps[i] = new HashMap<>();
        }

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

            int attributeId = 0;
            for (String value : record) {
                Map<String, IntArrayList> clusterMap = clusterMaps[attributeId];

                if (clusterMap.containsKey(value)) {
                    clusterMap.get(value).add(numRecords);
                }
                else {
                    IntArrayList newCluster = new IntArrayList();
                    newCluster.add(numRecords);
                    clusterMap.put(value, newCluster);
                }

                attributeId++;
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

        relationValues.add(relationData);
        return clusterMaps;
    }

    private List<PositionListIndex> fetchPositionListIndexes(int relationIndex, Map<String, IntArrayList>[] clusterMaps) {
        List<PositionListIndex> clustersPerAttribute = new ArrayList<>();
        for (int columnId = 0; columnId < clusterMaps.length; columnId++) {
            List<IntArrayList> clusters = new ArrayList<>();
            Map<String, IntArrayList> clusterMap = clusterMaps[columnId];

            if (!this.isNullEqualNull)
                clusterMap.remove(null);

            for (IntArrayList cluster : clusterMap.values())
                if (cluster.size() > 1)
                    clusters.add(cluster);

            AttributeBitSet attributeBitSet = new AttributeBitSet(relationIndex, columnId);
            clustersPerAttribute.add(new PositionListIndex(attributeBitSet, clusters));
        }

        return clustersPerAttribute;
    }

    private String format(AttributeBitSet abs) {

        String relName = relationNames.get(abs.getRelationIndex());
        String[] cols = attributeNames.get(abs.getRelationIndex());

        StringBuilder sb = new StringBuilder();
        sb.append(relName).append("[");

        BitSet bs = abs.getAttributeIndexSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(cols[i]).append(',');
        }
        sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    public void printUCC(UCCResult ucc) {
        for (AttributeBitSet lhs : ucc) {
            System.out.println(format(lhs));
        }
    }

    public void printIND(INDResult ind) {
        Iterator<AttributeBitSet> lhs = ind.lhs().iterator();
        Iterator<AttributeBitSet> rhs = ind.rhs().iterator();

        while (lhs.hasNext()) {
            System.out.println(format(lhs.next()) + " ⊆ " + format(rhs.next()));
        }
    }

    public void printFD(FDResult fd) {
        Iterator<AttributeBitSet> lhs = fd.lhs().iterator();
        Iterator<AttributeBitSet> rhs = fd.rhs().iterator();

        while (lhs.hasNext()) {
            System.out.println(format(lhs.next()) + " → " + format(rhs.next()));
        }
    }

    // Helper Methods
    private static int binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        long res = 1;
        for (int i = 1; i <= k; i++) {
            res = res * (n - i + 1) / i;
        }
        return (int) res;
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