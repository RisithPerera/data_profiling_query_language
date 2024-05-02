package de.metaserve.model.dpal;

import de.metaserve.model.constraints.*;
import lombok.Getter;

import java.util.*;

import static de.metaserve.model.dpal.Graph.INDRelation.*;

public class Graph {

    @Getter
    HashSet<Edge> edges = new HashSet<>();
    HashMap<String, Node> nodes = new HashMap<>();
    HashMap<Edge, SetMembership> setMembershipMap;

    public void computeSetMembership() {
        List<Graph> graphs = applyGraphTransformation();
        for (Graph graph : graphs) {
            if (graph.edges.size() > 3) {
                throw new RuntimeException("Graph Transformation failed!");
            }
            graph.setMembershipMap = graph.applyRules();
        }
        this.setMembershipMap = combineMembershipMaps(graphs);
    }

    private HashMap<Edge, SetMembership> combineMembershipMaps(List<Graph> graphs) {
        HashMap<Edge, SetMembership> result = new HashMap<>();
        for (Edge edge : edges){
            result.put(edge, SetMembership.getMembership(edge));
            for (Graph graph : graphs) {
                if (graph.hasEdge(edge)){
                    if(graph.setMembershipMap.get(edge).getValue() > result.get(edge).getValue())
                        result.put(edge, graph.setMembershipMap.get(edge));
                } else {//Is the case when the original nodes are renamed/ Currently only with FDS
                    if(!(edge instanceof FDEdge)) continue;
                    if(graph.hasEdge(new FDEdge(edge.leftName, edge.rightName+"RHS"))) {
                        FDEdge rhs = new FDEdge(edge.leftName, edge.rightName + "RHS");
                        if (graph.setMembershipMap.get(rhs).getValue() > result.get(edge).getValue())
                            result.put(edge, graph.setMembershipMap.get(rhs));
                    }
                    if(graph.hasEdge(new FDEdge(edge.leftName+"LHS", edge.rightName))) {
                        FDEdge lhs = new FDEdge(edge.leftName, edge.rightName + "LHS");
                        if (graph.setMembershipMap.get(lhs).getValue() > result.get(edge).getValue())
                            result.put(edge, graph.setMembershipMap.get(lhs));
                    }
                }
            }
        }
        return result;
    }

    private HashMap<Edge, SetMembership> applyRules() {
        setMembershipMap = new HashMap<>();
        for (Edge edge : edges) {
            SetMembership set = null;
            if(edge instanceof UCCEdge){
                set = applyUCCRules((UCCEdge) edge);
            } else if (edge instanceof FDEdge){
                set = applyFDRules((FDEdge) edge);
            } else if (edge instanceof INDEdge){
                set = applyINDRules((INDEdge) edge);
            } else {
                throw new RuntimeException("Found unkown type of edge!");
            }
            setMembershipMap.put(edge, set);
        }
        return setMembershipMap;
    }

    private SetMembership applyINDRules(INDEdge edge) {
        return null;
    }

    private SetMembership applyFDRules(FDEdge edge) {
        return null;
    }

    private SetMembership applyUCCRules(UCCEdge edge) {
        return null;
    }

    private List<Graph> applyGraphTransformation() {
        for (Edge edge : new ArrayList<>(edges)){
            if(!(edge instanceof FDEdge)) continue;
            Edge edgeLHS = new FDEdge(edge.leftName, edge.rightName+"RHS", (FDEdge) edge);
            Edge edgeRHS = new FDEdge(edge.leftName+"LHS", edge.rightName, (FDEdge) edge);
            removeEdge(edge);
            addEdge(edgeLHS);
            addEdge(edgeRHS);
        }

        List<Graph> graphs = new ArrayList<>();
        Graph newRestGraph = new Graph();
        for (Edge edge : edges) {
            if (edge instanceof INDEdge) continue;
            boolean isIsolated = true;
            for (Edge edge2 : edges){
                if (!(edge2 instanceof INDEdge)) continue;
                if (edge.equals(edge2)) continue;
                if (edge.leftName.equals(edge2.leftName)
                        || edge.leftName.equals(edge2.rightName)
                        || edge.rightName.equals(edge2.leftName)
                        || edge.rightName.equals(edge2.rightName)) {
                    isIsolated = false;
                    break;
                }

            }
            if (isIsolated)
                newRestGraph.addEdge(edge);
        }
        if(!newRestGraph.edges.isEmpty())
            graphs.add(newRestGraph);

        Set<INDEdge> edgesAlreadyChanged = new HashSet<>();
        for (Edge edge : edges){
            if(!(edge instanceof INDEdge)) continue;
            if(edgesAlreadyChanged.contains(edge)) continue;
            Set<INDEdge> set = allConnectedEdges((INDEdge) edge);
            edgesAlreadyChanged.addAll(set);
            Map<String, List<String>> graph = buildGraph(set);
            for(Pair<String,String> pair : generateAllPairs(getAllNodesFromEdges(set))){
                List<INDEdge> newEdges = findRelationEdges(graph, pair.getFirst(), pair.getSecond());
                for (INDEdge newEdge : newEdges){
                    Graph newGraph = new Graph();
                    newGraph.addEdge(newEdge);
                    for (Edge edge2 : edges){
                        if(edge2 instanceof INDEdge) continue;
                        if(edge2.contains(newEdge.leftName) || edge2.contains(newEdge.rightName)){
                            newGraph.addEdge(edge2);
                        }
                    }
                    graphs.add(newGraph);
                }
            }
        }
        return graphs;
    }

    enum INDRelation{
        SUBSET,
        SUPSET,
        UNDEFINABLE,
        UNKNOWN
    }
    private List<INDEdge> findRelationEdges(Map<String, List<String>> graph, String first, String second) {
        if(hasEdge(new INDEdge(first,second)))
            return List.of(new INDEdge(first, second));
        if(hasEdge(new INDEdge(second,first)))
            return List.of(new INDEdge(second, first));
        List<String> path = findShortestPath(graph, first, second);
        INDRelation relation = findPathRelation(path);
        List<INDEdge> result = new ArrayList<>();
        switch (relation){
            case SUPSET:
                result.add(new INDEdge(first, second));
                break;
            case SUBSET:
                result.add(new INDEdge(second, first));
                break;
            case UNDEFINABLE:
                result.add(new INDEdge(first, second));
                result.add(new INDEdge(second, first));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + relation);
        }
        return result;
    }

    private INDRelation findPathRelation(List<String> path) {
        String lastName = null;
        INDRelation relation = UNKNOWN;
        for (String nodeName : path){
            if(lastName == null){
                lastName = nodeName;
                continue;
            }
            INDRelation currentRelation;
            if(hasEdge(new INDEdge(lastName, nodeName))){
                currentRelation = SUPSET;
            } else if(hasEdge(new INDEdge(nodeName, lastName))){
                currentRelation = SUBSET;
            } else {
                throw new RuntimeException("Could not find relation between " + lastName + " and " + nodeName);
            }
            if(relation == UNKNOWN)
                relation = currentRelation;
            if(relation != currentRelation)
                return UNDEFINABLE;
        }
        return relation;
    }

    public Map<String, List<String>> buildGraph(Collection<INDEdge> edges) {
        Map<String, List<String>> graph = new HashMap<>();
        for (INDEdge edge : edges) {
            String from = edge.leftName;
            String to = edge.rightName;
            graph.putIfAbsent(from, new ArrayList<>());
            graph.putIfAbsent(to, new ArrayList<>());
            graph.get(from).add(to);
            graph.get(to).add(from);
        }
        return graph;
    }

    public List<String> findShortestPath(Map<String, List<String>> graph, String startNode, String endNode) {
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parentMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.offer(startNode);
        visited.add(startNode);
        parentMap.put(startNode, null);

        while (!queue.isEmpty()) {
            String currentNode = queue.poll();
            if (currentNode.equals(endNode)) {
                return reconstructPath(parentMap, endNode);
            }

            List<String> neighbors = graph.get(currentNode);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parentMap.put(neighbor, currentNode);
                        queue.offer(neighbor);
                    }
                }
            }
        }

        return null;
    }

    public static List<String> reconstructPath(Map<String, String> parentMap, String endNode) {
        List<String> path = new ArrayList<>();
        String current = endNode;
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private Set<Pair<String, String>> generateAllPairs(Collection<String> nodeNames){
        Set<Pair<String, String>> result = new HashSet<>();
        for (String nodeNameL : nodeNames){
            for (String nodeNameR : nodeNames){
                if(nodeNameL.equals(nodeNameR)) continue;
                if (result.contains(new Pair<>(nodeNameL, nodeNameR))
                        || result.contains(new Pair<>(nodeNameR, nodeNameL))) continue;
                result.add(new Pair<>(nodeNameL, nodeNameR));
            }
        }
        return result;
    }

    private <T extends Edge> Set<String> getAllNodesFromEdges(Collection<T> collection){
        HashSet<String> result = new HashSet<>();
        for (Edge edge : collection){
            result.add(edge.leftName);
            result.add(edge.rightName);
        }
        return result;
    }

    private Set<INDEdge> allConnectedEdges(INDEdge indEdge){
        HashSet<INDEdge> set = new HashSet<>();
        set.add(indEdge);
        return allConnectedEdges(set, indEdge);
    }
    private Set<INDEdge> allConnectedEdges(Set<INDEdge> set, INDEdge currentEdge){
        for (Edge edge : edges) {
            if (!(edge instanceof INDEdge)) continue;
            if (set.contains(edge)) continue;
            if (edge.leftName.equals(currentEdge.leftName)
                    || edge.leftName.equals(currentEdge.rightName)
                    || edge.rightName.equals(currentEdge.leftName)
                    || edge.rightName.equals(currentEdge.rightName)) {
                INDEdge parseEdge = (INDEdge) edge;
                set.add(parseEdge);
                allConnectedEdges(set, parseEdge);
            }
        }
        return set;
    }

    public void removeEdge(Edge edge){
        edges.remove(edge);
    }
    public boolean hasEdge(Edge edge){
        return edges.contains(edge);
    }
    public void addEdge(Edge edge){
        if(!hasEdge(edge))
            edges.add(edge);
    }

    public void addNode(String nodeName) {
        nodes.computeIfAbsent(nodeName, Node::new);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Collection<Edge> getMinimalEdges(){
        List<Edge> result = new ArrayList<>();
        for (Edge edge : setMembershipMap.keySet()){
            if(SetMembership.isMinimal(setMembershipMap.get(edge)))
                result.add(edge);
        }
        return result;
    }

    @Getter
    public enum SetMembership {
        F(0),
        F_PLUS(1),
        F_VALID(2),
        U(3),
        U_PLUS(4),
        I(5),
        I_PLUS(6),

        I_PLUS_ONE(7);

        private final int value;

        SetMembership(int value) {
            this.value = value;
        }

        public static SetMembership getMembership(Edge edge){
            if(edge instanceof INDEdge)
                return I_PLUS;
            if(edge instanceof FDEdge)
                return F;
            if(edge instanceof UCCEdge)
                return U;
            return null;
        }

        public static boolean isMinimal(SetMembership set){
            return set == F || set == U;
        }
    }
    public static Graph fromConditions(List<Condition> conditions){
        Graph graph = new Graph();
        for (Condition condition : conditions){
            if (condition == null)
                throw new RuntimeException("At least one condition was not correctly parsed!");
            if (!(condition instanceof Dependency))
                continue;
            switch (condition.getName()) {
                case IND.NAME:
                    IND indCondition = ((IND) condition);
                    Set<String> leftRightIND = new HashSet<>();
                    leftRightIND.addAll(indCondition.left);
                    leftRightIND.addAll(indCondition.right);
                    String[] arrayIND = leftRightIND.toArray(new String[leftRightIND.size()]);
                    graph.addEdge(new INDEdge(indCondition.leftName, indCondition.rightName, arrayIND));
                    break;
                case FD.NAME:
                    FD fdCondition = ((FD) condition);
                    Set<String> leftRightFD = new HashSet<>();
                    leftRightFD.addAll(fdCondition.left);
                    leftRightFD.addAll(fdCondition.right);
                    String[] arrayFD = leftRightFD.toArray(new String[leftRightFD.size()]);
                    graph.addEdge(new FDEdge(fdCondition.leftName, fdCondition.rightName,arrayFD));
                    break;
                case UCC.NAME:
                    UCC uccCondition = ((UCC) condition);
                    String[] arrayUCC = uccCondition.ccFunction.toArray(new String[uccCondition.ccFunction.size()]);
                    graph.addEdge(new UCCEdge(uccCondition.id,arrayUCC));
                    break;
                default:
                    break;
            }
        }
        return graph;
    }
}
