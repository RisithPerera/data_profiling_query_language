package de.metathesis.structures;

import de.metanome.algorithm_integration.input.RelationalInput;
import lombok.Getter;

import java.util.Objects;

public class Relation {
    @Getter private final int index;

    @Getter private RelationalInput relationalInput;

    //Relation Data
    private PositionListIndex[] unaryPLIs;
    private String[][] attributeValues;
    private int[][] compressedRecords;

    @Getter
    private boolean isDataLoaded = false;
    @Getter
    private boolean isPLICreated = false; //TODO: Need to use this in future

    public Relation(int index, RelationalInput relationalInput) {
        this.index = index;
        this.relationalInput = relationalInput;
    }

    public void markLoaded(String[][] attributeValues, PositionListIndex[] unaryPLIs, int[][] compressedRecords) {
        this.attributeValues = attributeValues;
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

    public int[][] getCompressedRecords() {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.compressedRecords;
    }

    public int getNumOfRecords() {
        assert isDataLoaded() : "Relation not loaded yet: " + this.relationalInput.relationName();
        return this.compressedRecords.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation other)) return false;

        //TODO: Need to check by File Path not Just by File name
        return Objects.equals(this.getName(), other.getName());
    }
}
