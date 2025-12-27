package de.metathesis;

import de.metanome.MetanomeHelper;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.structures.AttributeList;
import de.metathesis.structures.PositionListIndex;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public final class Preprocessor {

    private static final Preprocessor INSTANCE = new Preprocessor();
    private final HashMap<String, List<String>> tableMetadata = new HashMap<>();
    private final HashMap<String, List<PositionListIndex>> tablePlis = new HashMap<>();

    private final int inputRowLimit;
    private final boolean isNullEqualNull;

    private Preprocessor() {
        this.inputRowLimit = InputConfigurationSingleton.get().getFILE_MAX_ROWS();
        this.isNullEqualNull = InputConfigurationSingleton.get().getFILE_NULL_EQUALS_NULL();
    }

    public static Preprocessor getInstance() {
        return INSTANCE;
    }

    // Simulates reading a CSV and returning a Relation
    public synchronized List<PositionListIndex> loadRelation(String fileName) {
        if(tablePlis.containsKey(fileName)) {
            System.out.println("Return relations from " + fileName);
            return tablePlis.get(fileName);
        }

        try {
            System.out.println("Loading relations from " + fileName);
            RelationalInputGenerator input = MetanomeHelper.getInput(fileName);

            RelationalInput relationalInput = input.generateNewCopy();
            if (relationalInput == null) {
                throw new InputGenerationException("Input generation failed!");
            }

            String tableName = relationalInput.relationName();
            List<String> columns = relationalInput.columnNames();
            int numAttributes = columns.size();
            List<HashMap<String, IntArrayList>> clusters = calculateClusterMaps(relationalInput, numAttributes);
            List<PositionListIndex> plis = fetchPositionListIndexes(clusters, numAttributes);

            tableMetadata.put(fileName, columns);
            tablePlis.put(fileName, plis);
            return plis;
        } catch (InputGenerationException | AlgorithmConfigurationException | InputIterationException e) {
            throw new RuntimeException(e);
        }
    }

    private List<HashMap<String, IntArrayList>> calculateClusterMaps(RelationalInput relationalInput, int numAttributes) throws InputIterationException {
        List<HashMap<String, IntArrayList>> clusterMaps = new ArrayList<>();

        for (int i = 0; i < numAttributes; i++) {
            clusterMaps.add(new HashMap<String, IntArrayList>());
        }

        int numRecords = 0;
        while (relationalInput.hasNext() && (this.inputRowLimit <= 0 || this.inputRowLimit != numRecords)) {
            List<String> record = relationalInput.next();

            int attributeId = 0;
            for (String value : record) {
                HashMap<String, IntArrayList> clusterMap = clusterMaps.get(attributeId);

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
            if (numRecords == Integer.MAX_VALUE - 1)
                throw new RuntimeException("PLI encoding into integer based PLIs is not possible, because the number of records in the dataset exceeds Integer.MAX_VALUE. Use long based plis instead! (NumRecords = " + numRecords + " and Integer.MAX_VALUE = " + Integer.MAX_VALUE);
        }

        return clusterMaps;
    }

    private List<PositionListIndex> fetchPositionListIndexes(List<HashMap<String, IntArrayList>> clusterMaps, int numAttributes) {
        List<PositionListIndex> clustersPerAttribute = new ArrayList<>();
        for (int columnId = 0; columnId < clusterMaps.size(); columnId++) {
            List<IntArrayList> clusters = new ArrayList<>();
            HashMap<String, IntArrayList> clusterMap = clusterMaps.get(columnId);

            if (!this.isNullEqualNull)
                clusterMap.remove(null);

            for (IntArrayList cluster : clusterMap.values())
                if (cluster.size() > 1)
                    clusters.add(cluster);

            BitSet bs = new BitSet(numAttributes);
            bs.set(columnId);
            AttributeList attributeList = new AttributeList(bs);
            clustersPerAttribute.add(new PositionListIndex(attributeList, clusters));
        }
        return clustersPerAttribute;
    }
}