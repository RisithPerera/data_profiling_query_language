package de.metathesis;

import java.util.List;

public abstract class AbstractProfiler implements Profiler, Runnable {
    protected final Preprocessor dataProvider;
    protected final ResultListener resultListener;
    protected final String id;
    protected final int[][] input;

    protected AbstractProfiler(String id, Preprocessor provider, ResultListener listener, int[][] input) {
        this.id = id;
        this.dataProvider = provider;
        this.resultListener = listener;
        this.input = input;
    }

    // Profiler implementations implement profile(...) with actual logic
    @Override
    public abstract int[][] profile(int[][] raw);

    // Runnable wrapper for executor use
    @Override
    public void run() {
        // Typical flow: ask preprocessor for data, then call profile, then notify instructor.
        List<List<String>> data = dataProvider.get(input); // blocking, single-threaded inside Preprocessor
        // Convert preprocessed data back to int[][] if needed by profile; here we pass original raw as stated.
        int[][] result = profile(input);
        resultListener.onResult(id, result);
    }
}