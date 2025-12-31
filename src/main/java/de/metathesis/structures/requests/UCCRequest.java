package de.metathesis.structures.requests;

public final class UCCRequest {
    private final SearchSpace side;

    public UCCRequest(SearchSpace side) {
        this.side = side;
    }

    public SearchSpace side() {
        return side;
    }
}

