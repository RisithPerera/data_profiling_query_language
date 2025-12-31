package de.metathesis.structures.requests;

public class FDRequest {
    private final SearchSpace lhs;
    private final SearchSpace rhs;

    public FDRequest(SearchSpace lhs, SearchSpace rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public SearchSpace lhs() { return lhs; }
    public SearchSpace rhs() { return rhs; }
}
