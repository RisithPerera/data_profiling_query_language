package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metaserve.model.result.ResultSet;

import java.util.*;
import java.util.stream.Collectors;

public class Node {
    String name = "";
    List<ColumnCombination> result = new ArrayList<>();
    Set<String> negative = new HashSet<>();
    public List<Edge> edges = new ArrayList<>();
    boolean finished = false;

    //NEW:
    List<NewEdge> newEdges = new ArrayList<>();
    List<NewConstraint> newConstraints = new ArrayList<>();

    public Node(String name) {
        this.name = name;
    }

    public void add(ColumnCombination ids) {
        result.add(ids);
    }

    public void add(ColumnCombination ids, boolean negate) {
        if(negate)
            negative.add(ids.toString());
        else
            add(ids);
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    public void addNewEdge(NewEdge edge) {
        if(edge instanceof NewConstraint)
            newConstraints.add((NewConstraint) edge);
        else
            newEdges.add(edge);
    }
    public boolean hasEdge(Node rightNode, String type) {
        for (Edge edge : edges) {
            if (edge.type.equals(type) && edge.right.equals(rightNode))
                return true;
        }
        return false;
    }

    public Edge getEdge(Node rightNode, String type) {
        for (Edge edge : edges) {
            if (edge.type.equals(type) && edge.right.equals(rightNode))
                return edge;
        }
        return null;
    }

    public Edge getEdge(Node rightNode) {
        for (Edge edge : edges) {
            if (edge.right.equals(rightNode))
                return edge;
        }
        return null;
    }

    public NewEdge getNewEdge(Node rightNode, String type) {
        for (NewEdge edge : newEdges) {
            if (edge.type.equals(type) && edge.right.equals(rightNode))
                return edge;
        }
        return null;
    }

    public NewEdge getNewEdge(Node rightNode) {
        for (NewEdge edge : newEdges) {
            if (edge.right.equals(rightNode))
                return edge;
        }
        return null;
    }

    public List<String> compress() {
        List<String> result = new ArrayList<>();
        this.result.removeIf(cc -> negative.contains(cc.toString()));
        for (Edge edge : edges) {
            result.addAll(edge.getResults(this, this.result, negative));
        }
        if(edges.isEmpty() && this.result.isEmpty())
            return result;
        if(edges.isEmpty())
            result.addAll(columnsToString(this.result));

        return result;
    }

    private Collection<? extends String> columnsToString(List<ColumnCombination> resultColumns) {
        List<String> strList = new ArrayList<String>();
        for (ColumnCombination obj : resultColumns) {
            strList.add(obj.toString());
        }
        return strList;
    }

    public boolean isEndNode(){
        return edges.size() < 2;
    }

    public boolean isFinished() {
        return finished;
    }

    public Collection<Node> getNeighborNodes() {
        Set<Node> setOfNeighbors = new HashSet<>();
        for (Edge edge : edges){
            Node neighbor = edge.getOtherSide(this);
            setOfNeighbors.add(neighbor);
        }
        setOfNeighbors.remove(this);
        return setOfNeighbors;
    }

    public List<String> getResults() {
        return result.stream()
                .map(ColumnCombination::toString)
                .collect(Collectors.toList());
    }

    public List<String> getNewResults() {
        List<String> resultList = new ArrayList<>();
        if(newEdges.size() == 1){
            resultList.addAll(newEdges.get(0).getResultList(this));
        } else {
            boolean computedOnce = false;
            for (NewEdge edge : newEdges) {
                if (computedOnce)
                    break;
                computedOnce = true;
                if (edge instanceof INDEdge) {
                    resultList.addAll(newEdges.get(0).getResultList(this));
                } else if (edge instanceof FDEdge) {
                    resultList.addAll(newEdges.get(0).getResultList(this));
                }
            }
        }
        return resultList;
    }

    //Intersect all cc from all dependencies (edges)
    public Set<ColumnCombination> getAgreeSet() {
        Set<ColumnCombination> agreeSet = new HashSet<>(edges.get(0).getSide(edges.get(0).left.equals(this)));
        for (Edge edge : edges){
            Set<ColumnCombination> edgeSet = edge.getSide(edge.left.equals(this));
            agreeSet.retainAll(edgeSet);
        }
        return agreeSet;
    }

    Set<ColumnCombination> allowedCCs = new HashSet<>();
    public List<Node> getLocalMinimum() {
        Set<ColumnCombination> agreeSet = new HashSet<>(edges.get(0).getSide(edges.get(0).left.equals(this)));
        List<Node> effectedNeighbors = new ArrayList<>();
        for (Edge edge : edges){
            Set<ColumnCombination> edgeSet = edge.getSide(edge.left.equals(this));
            agreeSet.retainAll(edgeSet);
            if(!agreeSet.containsAll(edgeSet))
                effectedNeighbors.add(edge.getOtherSide(this));
        }
        allowedCCs = agreeSet;
        return effectedNeighbors;
    }

    public Set<ColumnCombination> getDisAgreeSet() {
        Set<ColumnCombination> agreeSet = new HashSet<>(edges.get(0).getSide(edges.get(0).left.equals(this)));
        Set<ColumnCombination>  disAgreeSet = new HashSet<>();
        for (Edge edge : edges){
            Set<ColumnCombination> edgeSet = edge.getSide(edge.left.equals(this));
            Set<ColumnCombination>  disAgreeSetTemp = new HashSet<>(agreeSet);

            agreeSet.retainAll(edgeSet);
            disAgreeSetTemp.removeAll(edgeSet);

            disAgreeSet.addAll(disAgreeSetTemp);
        }

        return disAgreeSet;
    }

     //@TODO test after minimize
    List<Constraint> constraints = new ArrayList<>();
    public List<Node> test() {
        for (Constraint constraint : constraints){
            for (ColumnCombination cc : new ArrayList<>(allowedCCs)) {
                Constraint.CCStatus status = constraint.test(cc);
                switch (status) {
                    case INVALID:
                        allowedCCs.remove(cc);
                        break;
                    case SPECIFY:
                    case GENERALIZE:
                        System.out.println(cc + " needs to be " + status.name() + " because of " + constraint.name());
                        break;
                    case VALID:
                    default:
                        continue;
                }
            }
        }
        return new ArrayList<>();
    }

    public void computeLocaleMin() {
        GlobalMinList minList = new GlobalMinList();
        for (NewEdge edge : newEdges){
            Set<ColumnCombination> edgeSet = edge.getNewSide(this);
            minList.add(edgeSet);
        }

        for (NewConstraint constraint : newConstraints){
            minList.apply(constraint);
        }

        Set<ColumnCombination> sccs = minList.getAllowedCCs();
        for (NewEdge edge : newEdges){
            edge.update(this,sccs);
        }
    }

    public Collection<? extends ResultSet> getNewTables() {
        List<ResultSet> tables = new ArrayList<>();
        for (NewEdge edge : newEdges) {
            if (edge instanceof INDEdge) {
                tables.add(edge.getResultTable());
            } else if (edge instanceof FDEdge) {
                tables.add(edge.getResultTable());
            }
        }
        return tables;
    }

    public void initNew() {
        for(NewEdge edge : newEdges){
            edge.updateNew();
        }
    }
}
