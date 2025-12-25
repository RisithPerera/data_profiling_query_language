package de.metathesis.structures;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;

public final class AttributeList {
    // The backing array of bit-packed column IDs
    private final long[] columns;

    // Cached hash code to ensure O(1) map lookups
    private final int cachedHash;

    public AttributeList(final long singleAttribute) {
        this(new long[]{singleAttribute});
    }

    // Private constructor to force use of factory/validation
    private AttributeList(long[] columns) {
        this.columns = columns;
        // Pre-calculate hash once. Arrays.hashCode is fast.
        this.cachedHash = Arrays.hashCode(columns);
    }

    /**
     * Factory method to create a key.
     * We clone the array to ensure immutability.
     */
    public static AttributeList of(long... columns) {
        long[] sorted = columns.clone();
        Arrays.sort(sorted); // <-- CRITICAL: Sort for canonical form
        return new AttributeList(sorted);
    }

    public AttributeList union(AttributeList other) {
        long[] attributes1 = this.columns.clone();
        long[] attributes2 = other.columns.clone();

        LongArrayList attributesUnion = new LongArrayList(attributes1.length + 1);
        int i = 0;
        int j = 0;
        while (i < attributes1.length || j < attributes2.length) {
            if (i >= attributes1.length) {
                attributesUnion.add(attributes2[j]);
                j++;
            } else if (j >= attributes2.length) {
                attributesUnion.add(attributes1[i]);
                i++;
            } else if (attributes1[i] == attributes2[j]) {
                attributesUnion.add(attributes1[i]);
                i++;
                j++;
            } else if (attributes1[i] < attributes2[j]) {
                attributesUnion.add(attributes1[i]);
                i++;
            } else {
                attributesUnion.add(attributes2[j]);
                j++;
            }
        }

        // Return a new ColumnCombination using the merged primitive array
        return new AttributeList(attributesUnion.toLongArray());
    }

    public boolean isSubsetOf(AttributeList other) {
        // Trivial case: A subset must be shorter or equal in size.
        if (this.columns.length > other.columns.length) {
            return false;
        }

        int i = 0; // Pointer for 'this' (the potential subset)
        int j = 0; // Pointer for 'other' (the potential superset)

        while (i < this.columns.length && j < other.columns.length) {
            if (this.columns[i] < other.columns[j]) {
                // 'this' element is smaller, but 'other' is sorted,
                // meaning this element is missing in 'other'.
                return false;
            } else if (this.columns[i] > other.columns[j]) {
                // 'this' element is larger, advance 'other' pointer to catch up.
                j++;
            } else {
                // Match found. Advance both pointers.
                i++;
                j++;
            }
        }

        // If we've successfully iterated through all elements of 'this' (i reached the end),
        // it means every element was found in 'other'.
        return i == this.columns.length;
    }

    public boolean isSupersetOf(AttributeList other) {
        // If this is a superset of 'other', then 'other' must be a subset of 'this'.
        return other.isSubsetOf(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeList that = (AttributeList) o;

        // Fast fail: if hashes don't match, arrays definitely don't match
        if (this.cachedHash != that.cachedHash) return false;

        // Finally compare actual array content
        return Arrays.equals(this.columns, that.columns);
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    // For debugging
    @Override
    public String toString() {
        return Arrays.toString(columns);
    }

    public int size() {
        return columns.length;
    }

    public long get(int index) {
        return columns[index];
    }
}
