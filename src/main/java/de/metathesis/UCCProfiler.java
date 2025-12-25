package de.metathesis;

public class UCCProfiler extends AbstractProfiler {
    public UCCProfiler(String id, Preprocessor provider, ResultListener listener, int[][] input) {
        super(id, provider, listener, input);
    }

    @Override
    public int[][] profile(int[][] raw) {
        // skeleton: return empty result placeholder; implement UCC logic here
        return new int[0][0];
    }
}