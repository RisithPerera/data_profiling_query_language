package de.metathesis;

public class INDProfiler extends AbstractProfiler {
    public INDProfiler(String id, Preprocessor provider, ResultListener listener, int[][] input) {
        super(id, provider, listener, input);
    }

    @Override
    public int[][] profile(int[][] raw) {
        // skeleton: return empty result placeholder; implement IND logic here
        return new int[0][0];
    }
}