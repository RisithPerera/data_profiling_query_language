package de.metaserve.model.dpal;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Node {
    String name;
    List<Edge> edges = new ArrayList<>();

    public Node(String name) {
        this.name = name;
    }

    public void addEdge(Edge edge){
        edges.add(edge);
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                '}';
    }

}
