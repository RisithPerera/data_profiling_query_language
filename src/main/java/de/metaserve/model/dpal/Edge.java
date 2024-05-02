package de.metaserve.model.dpal;

import de.metanome.algorithm_integration.results.Result;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public abstract class Edge {
    String leftName;
    String rightName;
    Edge originalEdge;

    List<Triple<Boolean, Edge, Boolean>> neighbors = new ArrayList<>();

    @Getter
    String[] searchSpace;

    @Getter @Setter
    List<Result> results;

    @Getter @Setter
    ResultsContainer resultsContainer;

    public Edge(String leftName, String rightName){
        this(leftName,rightName,null);
    }

    public Edge(String leftName, String rightName, Edge originalEdge){
        this(leftName,rightName,originalEdge,null);
    }

    public Edge(String leftName, String rightName, Edge originalEdge, String[] searchSpace){
        this.leftName = leftName;
        this.rightName = rightName;
        this.originalEdge = originalEdge;
        this.searchSpace = searchSpace;
    }

    public boolean contains(String nodeName){
        return nodeName.equals(leftName) || nodeName.equals(rightName);
    }

    public List<Triple<Boolean, Edge, Boolean>> getNeighborsWithDir() {
        return neighbors;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "leftName='" + leftName + '\'' +
                ", rightName='" + rightName + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
