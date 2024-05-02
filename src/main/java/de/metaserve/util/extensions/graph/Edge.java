package de.metaserve.util.extensions.graph;

public class Edge implements Comparable<Edge>{
    long relevance;
    Node left;
    Node right;
    public Edge(Node left, Node right){
        this.relevance = left.card - right.card;
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(Edge o) {
        return Math.toIntExact(o.relevance - relevance);
    }
}
