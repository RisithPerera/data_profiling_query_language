package de.metaserve.executor.min.graph.edge;

import de.metaserve.executor.min.graph.Node;
import de.metaserve.executor.min.results.ResultsContainer;
import de.metaserve.util.common.Triple;
import de.metathesis.profilers.results.Result;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Edge {
    public String leftName;
    public String rightName;
    @Setter
    protected Node leftNode;
    @Setter
    protected Node rightNode;
    public Edge originalEdge;

    List<Triple<Boolean, Edge, Boolean>> neighbors = new ArrayList<>();

    String[] searchSpace;

    public List<Result> results;

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

    public abstract Edge copy(HashMap<String, String> mapping);

    //Return a measurement of how each variable is used by other dependencies
    public abstract int degree();

    boolean marked = false;
    public void mark() {
        marked = true;
    }
    public boolean isMarked() {
        return marked;
    }


    public boolean isEndEdge(HashMap<String, Node> nodes) {
        return nodes.get(leftName).isEndNode() || nodes.get(rightName).isEndNode();
    }

    public String getOtherNeighbor(String name) {
        if (name.equals(leftName))
            return rightName;
        return leftName;
    }

    public String[] getSearchSpace() {
        return searchSpace;
    }

    public void setSearchSpace(String[] searchSpace) {
        this.searchSpace = searchSpace;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public ResultsContainer getResultsContainer() {
        return resultsContainer;
    }

    public void setResultsContainer(ResultsContainer resultsContainer) {
        this.resultsContainer = resultsContainer;
    }
}
