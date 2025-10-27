package de.metaserve.executor.min;

import java.util.*;


public class Node {
    String name;
    List<Edge> edgesOutgoing = new ArrayList<>();
    List<Edge> edgesIncoming = new ArrayList<>();
    Interval size;
    Interval cardinality;
    Integer isize;
    String isizeSource;
    Set<String> containsTargets = new HashSet<>();

    public Node(String name) {
        this.name = name;
    }

    public List<Edge> getEdges(){
        List<Edge> edges = new ArrayList<>(edgesIncoming);
        edges.addAll(edgesOutgoing);
        return edges;
    }
    public void addEdge(Edge edge){
        if(edge.leftName.equals(name))
            edgesOutgoing.add(edge);
        else if(edge.rightName.equals(name))
            edgesIncoming.add(edge);
        else
            throw new RuntimeException("Unkown edge in Node");
    }

    public void removeEdge(Edge edge) {
        if(edge.leftName.equals(name))
            edgesOutgoing.remove(edge);
        else if(edge.rightName.equals(name))
            edgesIncoming.remove(edge);
    }

    public Collection<String> indNeighbors() {
        Set<String> neighbors = new HashSet<>();
        for (Edge edge : edgesIncoming){
            if(edge instanceof INDEdge){
                neighbors.add(edge.rightName);
                neighbors.add(edge.leftName);
            }
        }
        for (Edge edge : edgesOutgoing){
            if(edge instanceof INDEdge){
                neighbors.add(edge.rightName);
                neighbors.add(edge.leftName);
            }
        }
        neighbors.remove(this.name);
        return neighbors;
    }

    public Collection<String> neighbors() {
        Set<String> neighbors = new HashSet<>();
        for (Edge edge : edgesIncoming){
            neighbors.add(edge.rightName);
            neighbors.add(edge.leftName);
        }
        for (Edge edge : edgesOutgoing){
            neighbors.add(edge.rightName);
            neighbors.add(edge.leftName);
        }
        neighbors.remove(this.name);
        return neighbors;
    }

    public int degree() {
        Set<Edge> allEdges = new HashSet<>();
        allEdges.addAll(edgesIncoming);
        allEdges.addAll(edgesOutgoing);
        return allEdges.size();
    }

    public boolean isEmpty() {
        return edgesIncoming.isEmpty() && edgesOutgoing.isEmpty();
    }

    public boolean isEndNode() {
        return edgesIncoming.size() + edgesOutgoing.size() <= 1;
    }
    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(name, node.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Node copy(String s) {
        Node newNode = new Node(s);
        newNode.edgesIncoming = new ArrayList<>(edgesIncoming);
        newNode.edgesOutgoing = new ArrayList<>(edgesOutgoing);
        newNode.size = size;
        newNode.cardinality = cardinality;
        newNode.isize = isize;
        newNode.isizeSource = isizeSource;
        newNode.containsTargets = containsTargets;

        return newNode;
    }

    public Set<Edge> edges() {
        Set<Edge> allEdges = new HashSet<>();
        allEdges.addAll(edgesIncoming);
        allEdges.addAll(edgesOutgoing);
        return allEdges;
    }

    public INDEdge indEdge(String nodeName) {
        for (Edge edge : edgesIncoming){
            if(edge instanceof INDEdge && (edge.rightName.equals(nodeName) || edge.leftName.equals(nodeName))){
                return (INDEdge) edge;
            }
        }
        for (Edge edge : edgesOutgoing){
            if(edge instanceof INDEdge && (edge.rightName.equals(nodeName) || edge.leftName.equals(nodeName))){
                return (INDEdge) edge;
            }
        }
        throw new RuntimeException("No such edge found!");
    }

    public List<Edge> getEdgesIncoming() {
        return edgesIncoming;
    }

    public List<Edge> getEdgesOutgoing() {
        return edgesOutgoing;
    }

    public String getName() {
        return name;
    }

    public Interval getSize() {
        return size;
    }

    public void setSize(Interval size) {
        this.size = size;
    }

    public Interval getCardinality() {
        return cardinality;
    }

    public void setCardinality(Interval cardinality) {
        this.cardinality = cardinality;
    }

    public Integer getIsize() {
        return isize;
    }

    public void setIsize(Integer isize, String source) {
        this.isize = isize;
        this.isizeSource = source;
    }

    public void clearIsize() {
        this.isize = null;
        this.isizeSource = null;
    }

    public String getIsizeSource() {
        return isizeSource;
    }

    public Set<String> getContainsTargets() {
        return containsTargets;
    }

    public void addContainsTarget(String target) {
        containsTargets.add(target);
    }
}
