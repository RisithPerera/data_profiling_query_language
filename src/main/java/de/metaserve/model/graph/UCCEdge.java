package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;

import java.util.Collection;
import java.util.HashSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UCCEdge extends NewEdge{
    Node node;
    Set<ColumnCombination> ccs = new HashSet<>();
    boolean negate = false;
    public UCCEdge() {
        this(false);
    }

    public UCCEdge(boolean negate) {
        super();
        type = "UCC";
        this.negate = negate;
    }

    public void setNode(Node node){
        this.node = node;
    }

    public void add(ColumnCombination ccs) {
        this.ccs.add(ccs);
    }

    public void addAll(Collection<ColumnCombination> ccs) {
        this.ccs.addAll(ccs);
    }

    public void update(Set<ColumnCombination> sccs) {
        ccs.removeIf(sccs::contains);
    }

    @Override
    public List<String> getResultList(Node node) {
        return ccs.stream().map(ColumnCombination::toString).collect(Collectors.toList());
    }

    @Override
    public Set<ColumnCombination> getSide(Node node) {
        return ccs;
    }

    @Override
    public Set<ColumnCombination> getNewSide(Node node) {
        return ccs;
    }
}
