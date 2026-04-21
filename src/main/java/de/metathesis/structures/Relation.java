package de.metathesis.structures;

import de.metanome.algorithm_integration.input.RelationalInput;
import lombok.Getter;

import java.util.*;

public class Relation {
    @Getter private final int index;

    @Getter private RelationalInput relationalInput;

    //Relation Data
    private PositionListIndex[] unaryPLIs;
    private String[][] attributeValues;
    private String[][] sortedAttributeValues;
    private Map<String, BitSet> invertedAttributeValues;
    private int[][] compressedRecords;

    @Getter
    private volatile boolean isDataLoaded = false;

    @Getter
    private volatile boolean isPLICreated = false; //TODO: Need to use this in future

    public Relation(int index, RelationalInput relationalInput) {
        this.index = index;
        this.relationalInput = relationalInput;
    }

    public void markLoaded(String[][] attributeValues, PositionListIndex[] unaryPLIs, int[][] compressedRecords) {
        this.attributeValues = attributeValues;
        this.sortedAttributeValues = buildSortedValueSets(attributeValues);
        this.invertedAttributeValues = buildInvertedIndex(sortedAttributeValues);
        this.unaryPLIs = unaryPLIs;
        this.compressedRecords = compressedRecords;
        this.isDataLoaded = true;
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

    public PositionListIndex[] getUnaryPLIs() {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.unaryPLIs;
    }

    public String[][] getAttributeValues() {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.attributeValues;
    }

    public String[] getSortedAttributeSet(int col) {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.sortedAttributeValues[col];
    }

    public Map<String, BitSet> getInvertedAttributeValues() {
        assert isDataLoaded : "Relation not loaded yet";
        return invertedAttributeValues;
    }

    public int[][] getCompressedRecords() {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.compressedRecords;
    }

    public int getNumOfRecords() {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.compressedRecords.length;
    }

    private String[][] buildSortedValueSets(String[][] columns) {
        String[][] sorted = new String[columns.length][];

        for (int col = 0; col < columns.length; col++) {
            TreeSet<String> valueSet = new TreeSet<>();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation other)) return false;

        //TODO: Need to check by File Path not Just by File name
        return Objects.equals(this.getName(), other.getName());
    }
}
