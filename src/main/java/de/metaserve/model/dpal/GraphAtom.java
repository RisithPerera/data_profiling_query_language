package de.metaserve.model.dpal;

import java.util.*;

public class GraphAtom {

    int inds = 0;
    int fds = 0;
    int uccs = 0;
    public Graph graph;
    int hash = 0;
    public GraphAtom(int hash){
        this.hash = hash;
    }
    public GraphAtom(Graph graph){
        this.graph = graph;
        for (Edge edge : graph.edges){
            if(edge instanceof UCCEdge)
                uccs++;
            else if (edge instanceof FDEdge)
                fds++;
            else
                inds++;
        }
        List<Graph.SetMembership> list = new ArrayList<>(graph.setMembershipMap.values());
        if (list.contains(Graph.SetMembership.I_PLUS)){
            while (list.contains(Graph.SetMembership.I_PLUS)){
                list.remove(Graph.SetMembership.I_PLUS);
            }
            list.add(Graph.SetMembership.I_PLUS);
        }
        if (list.contains(Graph.SetMembership.I_MINUS)){
            while (list.contains(Graph.SetMembership.I_MINUS)){
                list.remove(Graph.SetMembership.I_MINUS);
            }
            list.add(Graph.SetMembership.I_MINUS);
        }
        list.sort(null);

        hash = (list).hashCode();

    }

    @Override
    public String toString() {
        return "Graph(" + hash + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphAtom graphAtom = (GraphAtom) o;
        return hash == graphAtom.hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
