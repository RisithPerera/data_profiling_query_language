package de.metanome;

public enum DependencyType {
    IND,
    FD,
    UCC,
    CARD;

    public String toString() {
        switch (this) {
            case IND:
                return "IND";
            case FD:
                return "FD";
            case UCC:
                return "UCC";
            case CARD:
                return "CARD";
            default:
                throw new IllegalArgumentException("Invalid DependencyType: " + this);
        }
    }
}
