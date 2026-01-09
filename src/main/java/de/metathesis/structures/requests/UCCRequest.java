package de.metathesis.structures.requests;

public final class UCCRequest implements Request{
    private final SearchSpace lhs;

    public UCCRequest(SearchSpace lhs) {
        this.lhs = lhs;
    }

    @Override
    public SearchSpace lhs() {
        return lhs;
    }

    @Override
    public SearchSpace rhs() {
        return lhs; //return same
    }
}

