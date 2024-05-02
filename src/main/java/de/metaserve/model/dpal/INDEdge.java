package de.metaserve.model.dpal;


public class INDEdge extends Edge{

    public INDEdge(String left, String right) {
        super(left, right);
    }
    public INDEdge(String left, String right, INDEdge edge) {
        super(left, right,edge);
    }

    public INDEdge(String left, String right, String[] searchSpace) {
        super(left, right,null,searchSpace);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "leftName='" + leftName + '\'' +
                ", rightName='" + rightName + '\'' +
                ", type='IND'" +
                '}';
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
}
