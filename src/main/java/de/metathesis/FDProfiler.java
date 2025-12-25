package de.metathesis;

public class FDProfiler extends AbstractProfiler {
    public FDProfiler(String id, Preprocessor provider, ResultListener listener, int[][] input) {
        super(id, provider, listener, input);
    }

    @Override
    public int[][] profile(int[][] raw) {
        // skeleton: return empty result placeholder; implement FD logic here
        return new int[0][0];
    }
}
