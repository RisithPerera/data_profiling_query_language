package de.metaserve.executor.min.graph.edge;

import java.util.HashMap;

public class FDEdge extends Edge{

    public FDEdge(String left, String right) {
        super(left, right);
    }

    public FDEdge(String left, String right, FDEdge edge) {
        super(left, right, edge);
    }

    public FDEdge(String left, String right, String[] searchSpace) {
        super(left, right, null, searchSpace);
    }


    @Override
    public String toString() {
        return "FD(" + leftName + ',' + rightName + ')';
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
        return new FDEdge(mapping.get(leftName), mapping.get(rightName));
    }
}
