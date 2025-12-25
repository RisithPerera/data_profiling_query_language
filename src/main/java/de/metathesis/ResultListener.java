package de.metathesis;

public interface ResultListener {
    /**
     * Called by profilers to publish their results back to Instructor.
     * profilerId can be used to identify which profiler instance produced the result.
     */
    void onResult(String profilerId, int[][] result);
}
