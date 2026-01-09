package de.metathesis.structures.requests;

public final class UCCRequest {
    private final SearchSpace lhs;

    public UCCRequest(SearchSpace lhs) {
        this.lhs = lhs;
    }

    public SearchSpace lhs() {
        return lhs;
    }
}

