package de.metathesis.structures;

import de.metanome.algorithm_integration.ColumnIdentifier;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Stores column data (List<String>) efficiently using bit-packed keys
 * and maintains metadata for human-readable lookup.
 */
public class ColumnStore {

    // 1. The primary data store (from ColumnStore)
    // Maps bit-packed long key to the List of String values.
    private final Long2ObjectOpenHashMap<PositionListIndex> dataStore = new Long2ObjectOpenHashMap<>();

    // 2. The metadata store (from MetadataStore)
    // Maps the same bit-packed long key to the ColumnMetadata object.
    private final Long2ObjectOpenHashMap<ColumnIdentifier> metadataStore = new Long2ObjectOpenHashMap<>();

    // --- Inner Metadata Class ---

    // --- Key Packing Utilities ---

    /**
     * Combines tableId and colId into a single high-performance long key.
     * Uses bit shifting: tableId occupies the high 32 bits, colId the low 32 bits.
     */
    public static long composeKey(int tableId, int colId) {
        return ((long) tableId << 32) | (colId & 0xFFFFFFFFL);
    }

    /**
     * Extracts the Table ID from the packed key.
     */
    public static int extractTableId(long key) {
        return (int) (key >>> 32);
    }

    /**
     * Extracts the Column ID from the packed key.
     */
    public static int extractColId(long key) {
        return (int) key;
    }

    // --- Core Operations ---

    /**
     * Adds a value to the specified column and ensures metadata is stored.
     * This is used during the data loading/profiling phase.
     */
   /* public void addValue(int tableId, int colId, String tableName, String columnName, String value) {
        long key = composeKey(tableId, colId);

        // 1. Store the actual data (O(1) lookup/access)
        dataStore.computeIfAbsent(key, k -> new ArrayList<>()).add(value);

        // 2. Store metadata (only needs to be stored once per column)
        if (!metadataStore.containsKey(key)) {
            metadataStore.put(key, new ColumnIdentifier(tableName, columnName));
        }
    }*/

    /**
     * Retrieves the List of String values for a column using IDs.
     */
    public PositionListIndex getPLI(int tableId, int colId) {
        return dataStore.get(composeKey(tableId, colId));
    }

    /**
     * Retrieves the List of String values for a column using the packed key.
     */
    public PositionListIndex getColumn(long packedKey) {
        return dataStore.get(packedKey);
    }

    /**
     * Retrieves the human-readable metadata for a column using the packed key.
     */
    public ColumnIdentifier getMetadata(long packedKey) {
        return metadataStore.get(packedKey);
    }
}