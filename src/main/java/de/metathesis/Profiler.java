package de.metathesis;

public interface Profiler {
    /**
     * Accepts raw matrix input and returns profile results as int[][]
     * (no business logic implemented here in skeleton).
     */
    int[][] profile(int[][] raw);
}