package de.metathesis.structures;

import de.metanome.algorithm_integration.input.RelationalInput;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.*;

public class Relation {
    @Getter
    private final int index;

    @Getter
    private RelationalInput relationalInput;

    //Relation Data
    private volatile int numOfRecords = -1;
    private volatile String[][] attributeValues;
    private volatile String[][] uniqueAttributeValues; //For INDs
    private volatile Map<String, BitSet> invertedAttributeValues; //For INDs
    private volatile PositionListIndex[] unaryPLIs;
    private volatile int[][] compressedRecords;

    public Relation(int index, RelationalInput relationalInput) {
        this.index = index;
        this.relationalInput = relationalInput;
    }

    public void loadData(String[][] attributeValues, int numOfRecords) {
        this.attributeValues = attributeValues; // volatile write — visible to all threads immediately
        this.numOfRecords = numOfRecords;
    }

    public boolean isDataLoaded() {
        return this.attributeValues != null;
    }

    public String getName() {
        String fullName = this.relationalInput.relationName();
        return fullName.endsWith(".csv") ? fullName.replace(".csv", "") : fullName;
    }

    public String[] getAttributeNames() {
        return this.relationalInput.columnNames().toArray(new String[0]);
    }

    public int getNumOfAttributes() {
        return this.relationalInput.columnNames().size();
    }

    public String[][] getAttributeValues() {
        assert isDataLoaded() : "Relation Data not loaded yet: " + this.relationalInput.relationName();
        return this.attributeValues;
    }

    public String[][] getUniqueAttributeValues() {
        assert isDataLoaded() : "Relation Data not loaded yet: " + this.relationalInput.relationName();
        if (this.uniqueAttributeValues == null) {
            synchronized (this) {
                if (this.uniqueAttributeValues == null) {
                    this.uniqueAttributeValues = buildUniqueValueSets(getAttributeValues());
                }
            }
        }
        return this.uniqueAttributeValues;
    }

    public Map<String, BitSet> getInvertedAttributeValues() {
        assert isDataLoaded() : "Relation Data not loaded yet: " + this.relationalInput.relationName();
        if (this.invertedAttributeValues == null) {
            synchronized (this) {
                if (this.invertedAttributeValues == null) {
                    this.invertedAttributeValues = buildInvertedIndex(getUniqueAttributeValues());
                }
            }
        }
        return this.invertedAttributeValues;
    }

    public PositionListIndex[] getUnaryPLIs() {
        assert isDataLoaded() : "Relation Data not loaded yet: " + this.relationalInput.relationName();
        if (this.unaryPLIs == null) {
            synchronized (this) {
                if (this.unaryPLIs == null) {
                    this.unaryPLIs = buildUnaryPLIs(index, attributeValues);
                }
            }
        }
        return this.unaryPLIs;
    }

    public int[][] getCompressedRecords() {
        assert isDataLoaded() : "Relation Data not loaded yet: " + this.relationalInput.relationName();
        if (this.compressedRecords == null) {
            synchronized (this) {
                if (this.compressedRecords == null) {
                    this.compressedRecords = buildCompressedRecords(getUnaryPLIs());
                }
            }
        }
        return this.compressedRecords;
    }

    public boolean isAllColumnUnique() {
        assert isDataLoaded() : "Relation Data not loaded yet: " + this.relationalInput.relationName();
        return Arrays.stream(getUnaryPLIs()).allMatch(PositionListIndex::isUnique);
    }

    private String[][] buildUniqueValueSets(String[][] columns) {
        String[][] sorted = new String[columns.length][];

        for (int col = 0; col < columns.length; col++) {
            Set<String> valueSet = new HashSet<>();

            for (int r = 0; r < columns[col].length; r++) {
                String v = columns[col][r];
                if (v != null && !v.isEmpty()) {
                    valueSet.add(v);
                }
            }

            sorted[col] = valueSet.toArray(new String[0]);
        }
        return sorted;
    }

    private Map<String, BitSet> buildInvertedIndex(String[][] sortedCols) {
        Map<String, BitSet> invertedRhs = new HashMap<>();
        for (int col = 0; col < getNumOfAttributes(); col++) {
            for (String value : sortedCols[col]) {
                invertedRhs.computeIfAbsent(value, k -> new BitSet()).set(col);
            }
        }
        return invertedRhs;
    }

    private PositionListIndex[] buildUnaryPLIs(int relationIndex, String[][] columns){
        String nullValue = InputConfigurationSingleton.get().getFILE_NULL_STRING();
        boolean isNullEqualNull = InputConfigurationSingleton.get().getFILE_NULL_EQUALS_NULL();

        int numOfAttributes = columns.length;

        PositionListIndex[] plis = new PositionListIndex[numOfAttributes];
        for (int columnIndex = 0; columnIndex < numOfAttributes; columnIndex++) {
            Map<String, IntArrayList> clusterMap = new HashMap<>();

            int rowIndex = 0;
            for(String value : columns[columnIndex]) {
                if (Objects.equals(value, nullValue) && !isNullEqualNull) {
                    rowIndex++;
                    continue;
                }
                clusterMap.computeIfAbsent(value, k -> new IntArrayList()).add(rowIndex);
                rowIndex++;
            }

            List<IntArrayList> clusters = new ArrayList<>();
            int numUniqueValues = 0;
            for (IntArrayList cluster : clusterMap.values()) {
                if (cluster.size() > 1) {
                    clusters.add(cluster);
                }else{
                    numUniqueValues++;
                }
            }

            AttributeBitSet attributeBitSet = new AttributeBitSet(relationIndex, columnIndex);

            PositionListIndex pli = new PositionListIndex(attributeBitSet, clusters);
            pli.setNumUniqueValues(numUniqueValues);
            pli.setNumOfRecords(columns[0].length);

            plis[columnIndex] = pli;
        }

        return plis;
    }

    private int[][] buildCompressedRecords(PositionListIndex[] plis) {
        // Direct [row][col] matrix
        int[][] compressedRecords = new int[this.numOfRecords][plis.length];

        // Fill all with -1 (unique/singleton values)
        for (int[] row : compressedRecords) {
            Arrays.fill(row, -1);
        }

        // For each column's PLI, assign cluster IDs directly into row-col position
        for (int attr = 0; attr < plis.length; attr++) {
            List<IntArrayList> clusters = plis[attr].getClusters();
            for (int clusterId = 0; clusterId < clusters.size(); clusterId++) {
                for (int recordId : clusters.get(clusterId)) {
                    compressedRecords[recordId][attr] = clusterId;
                }
            }
        }

        return compressedRecords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation other)) return false;

        //TODO: Need to check by File Path not Just by File name
        return Objects.equals(this.getName(), other.getName());
    }
}
