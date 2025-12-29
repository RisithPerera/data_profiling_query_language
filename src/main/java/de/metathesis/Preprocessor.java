package de.metathesis;

import de.metanome.MetanomeHelper;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.structures.ImmutableBitSet;
import de.metathesis.structures.PositionListIndex;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

public final class Preprocessor {

    private static final Preprocessor INSTANCE = new Preprocessor();

    private final Object2IntMap<String> relationToIndex = new Object2IntOpenHashMap<>();

    private final List<String> relationNames = new ArrayList<>();
    private final List<String[]> attributeNames = new ArrayList<>();
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

    // Simulates reading a CSV and returning a Relation
    public synchronized PositionListIndex[] getPositionListIndexesOf(int relationIndex) throws InputIterationException {

        if(relationPLIsMap.containsKey(relationIndex)) {
            System.out.println("Return Cached PLI: " + relationIndex);
            return relationPLIsMap.get(relationIndex);
        }

        RelationalInput relationalInput = relationalInputs.get(relationIndex);
        assert relationalInput != null : "Initialization is important!";

        int numAttributes = attributeNames.get(relationIndex).length;

        List<Map<String, IntArrayList>> clusters = calculateClusterMaps(relationalInput, numAttributes);
        List<PositionListIndex> plis = fetchPositionListIndexes(clusters);

        PositionListIndex[] plisArray = plis.toArray(new PositionListIndex[0]);

        relationPLIsMap.put(relationIndex, plisArray);
        return plisArray;
    }

    private List<Map<String, IntArrayList>> calculateClusterMaps(RelationalInput relationalInput, int numAttributes) throws InputIterationException {
        List<Map<String, IntArrayList>> clusterMaps = new ArrayList<>();

        for (int i = 0; i < numAttributes; i++) {
            clusterMaps.add(new HashMap<>());
        }

        int numRecords = 0;
        while (relationalInput.hasNext() && (this.inputRowLimit <= 0 || this.inputRowLimit != numRecords)) {
            List<String> record = relationalInput.next();

            int attributeId = 0;
            for (String value : record) {
                Map<String, IntArrayList> clusterMap = clusterMaps.get(attributeId);

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

        return clusterMaps;
    }

    private List<PositionListIndex> fetchPositionListIndexes(List<Map<String, IntArrayList>> clusterMaps) {
        List<PositionListIndex> clustersPerAttribute = new ArrayList<>();
        for (int columnId = 0; columnId < clusterMaps.size(); columnId++) {
            List<IntArrayList> clusters = new ArrayList<>();
            Map<String, IntArrayList> clusterMap = clusterMaps.get(columnId);

            if (!this.isNullEqualNull)
                clusterMap.remove(null);

            for (IntArrayList cluster : clusterMap.values())
                if (cluster.size() > 1)
                    clusters.add(cluster);

            ImmutableBitSet immutableBitSet = new ImmutableBitSet(columnId);
            clustersPerAttribute.add(new PositionListIndex(immutableBitSet, clusters));
        }

        return clustersPerAttribute;
    }
}