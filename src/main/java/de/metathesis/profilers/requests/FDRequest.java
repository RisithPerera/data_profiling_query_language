package de.metathesis.profilers.requests;

public class FDRequest implements Request {
    private final SearchSpace lhs;
    private final SearchSpace rhs;

    public FDRequest(SearchSpace lhs, SearchSpace rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public SearchSpace lhs() { return lhs; }

    @Override
    public SearchSpace rhs() { return rhs; }
}
