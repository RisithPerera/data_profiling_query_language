package de.metaserve.util.extensions.graph;


import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.singletons.InputConfigurationSingleton;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Node {
    String name;
    long card = 0;

    public List<Edge> getIn() {
        return new ArrayList<>(in);
    }

    public List<Edge> getOut() {
        return new ArrayList<>(out);
    }

    List<Edge> in = new ArrayList<>();
    List<Edge> out = new ArrayList<>();

    public Node(String name) {
        this.name = name;
        this.card = InputConfiguration.getCard(name);
    }

    public void addInEdge(Edge edge) {
        in.add(edge);
    }

    public void addOutEdge(Edge edge) {
        out.add(edge);
    }

    public boolean isStartNode() {
        return in.isEmpty();
    }

    public boolean isEndNode() {
        return out.isEmpty();
    }

    public int inSize(){
        return in.size();
    }

    public int outNodes(){
        return out.size();
    }

    public void removeInEdge(Node node) {
        in.removeIf(edge -> edge.left.equals(node));
    }

    public void removeOutEdge(Node node) {
        out.removeIf(edge -> edge.right.equals(node));
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

    public boolean isEmpty() {
        return isEndNode() && isStartNode();
    }

    public int outSize() {
        return out.size();
    }

    public boolean hasNeighbors() {
        return !isEmpty();
    }

    public Edge getEdge(Node to) {
        for (Edge edge : out){
            if(edge.left.equals(to) || edge.right.equals(to)){
                return edge;
            }
        }
        return null;
    }
}