package de.metaserve.executor.min.graph.edge;

import java.util.HashMap;

public class UCCEdge extends Edge{

    public UCCEdge(String left) {
        super(left, left);
    }

    public UCCEdge(String left, UCCEdge edge) {
        super(left, left, edge);
    }

    public UCCEdge(String left, String[] searchSpace) {
        super(left, left, null, searchSpace);
    }

    @Override
    public String toString() {
        return "UCC(" + leftName + ')';
    }
    @Override
    public boolean equals(Object obj) {
        if(super.equals(obj))
            return true;
        return hashCode() == obj.hashCode();
    }
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public Edge copy(HashMap<String, String> mapping) {
        return new UCCEdge(mapping.get(leftName));
    }
}