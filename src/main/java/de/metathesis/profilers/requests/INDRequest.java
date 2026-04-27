package de.metathesis.profilers.requests;

public final class INDRequest implements Request{
    private final SearchSpace lhs;
    private final SearchSpace rhs;

    public INDRequest(SearchSpace lhs, SearchSpace rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public SearchSpace lhs() { return lhs; }
    public SearchSpace rhs() { return rhs; }
}
