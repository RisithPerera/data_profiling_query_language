package de.metaserve.model.dpal;

import de.metaserve.model.constraints.*;
import de.metaserve.util.common.Pair;
import de.metaserve.util.common.Triple;

import java.util.*;
import java.util.stream.Collectors;

import static de.metaserve.model.dpal.Graph.INDRelation.*;

public class Graph {

    HashSet<Edge> edges = new HashSet<>();
    HashMap<String, Node> nodes = new HashMap<>();
    HashMap<Edge, SetMembership> setMembershipMap;
    Set<String> existsNodes = new HashSet<>();
    GraphMetadata metadata = new GraphMetadata();

    public static HashMap<GraphAtom, List<GraphAtom>> decomposedGraphMap = new HashMap<>();
    public void computeSetMembership() {
        List<Graph> graphs = applyGraphDecomposition();
        for (Graph graph : graphs) {
            graph.setMembershipMap = graph.applyRules();
            GraphAtom atom = new GraphAtom(graph);
            decomposedGraphMap.putIfAbsent(atom, new ArrayList<>());
            decomposedGraphMap.get(atom).add(atom);
        }
        this.setMembershipMap = combineMembershipMaps(graphs);
        //Graph clean up
        edges.clear();
        nodes.clear();
        for (Edge edge: setMembershipMap.keySet()){
            addEdge(edge);
        }
    }

    private HashMap<Edge, SetMembership> combineMembershipMaps(List<Graph> graphs) {
        HashMap<Edge, SetMembership> result = new HashMap<>();
        for (Edge edge : edges){
            Edge realEdge = edge;
            if(edge.originalEdge != null)
                realEdge = edge.originalEdge;
            if (!result.containsKey(realEdge))
                result.put(realEdge, SetMembership.getMembership(edge));
            for (Graph graph : graphs) {
                if (graph.hasEdge(edge)){
                    if(graph.setMembershipMap.get(edge).getValue() > result.get(realEdge).getValue())
                        result.put(realEdge, graph.setMembershipMap.get(edge));
                } else if (edge instanceof INDEdge) {
                    if (graph.nodes.containsKey(edge.leftName) || graph.nodes.containsKey(edge.rightName)){
                        for (Edge poINDEdge : graph.edges){
                            if (poINDEdge instanceof INDEdge){
                                if (poINDEdge.contains(edge.leftName) || poINDEdge.contains(edge.rightName))
                                    if(graph.setMembershipMap.get(poINDEdge).getValue() > result.get(realEdge).getValue())
                                        result.put(realEdge, graph.setMembershipMap.get(poINDEdge));
                            }
                        }
                    }
                }/**else {//Is the case when the original nodes are renamed/ Currently only with FDS
                 if(!(edge instanceof FDEdge)) continue;
                 if (transformationMap.containsKey(edge)){
                 for (Edge renamedEdge : transformationMap.get(edge)){
                 if(graph.hasEdge(renamedEdge)){
                 if (graph.setMembershipMap.get(renamedEdge).getValue() > result.get(edge).getValue())
                 result.put(edge, graph.setMembershipMap.get(renamedEdge));
                 }
                 }
                 }
                 }
                 **/
            }
        }
        for (Graph graph : graphs){
            for (String nodeName : graph.getExistsNodes()){
                for (Edge edge : graph.edges){
                    if (edge.contains(nodeName)){
                        if (edge.leftName.equals(nodeName)){
                            if (edge.originalEdge != null)
                                existsNodes.add(edge.originalEdge.leftName);
                            else
                                existsNodes.add(edge.leftName);
                        } else {
                            if (edge.originalEdge != null)
                                existsNodes.add(edge.originalEdge.rightName);
                            else
                                existsNodes.add(edge.rightName);
                        }
                        break;
                    }
                }
            }
        }

        for (Graph graph : graphs){
            metadata.add(graph.getMetadata());
        }
        return result;
    }

    private HashMap<Edge, SetMembership> applyRules2() {
        setMembershipMap = new HashMap<>();
        List<UCCEdge> uccs = new ArrayList<>();
        List<FDEdge> fds = new ArrayList<>();
        List<INDEdge> inds = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof UCCEdge) uccs.add((UCCEdge) edge);
            else if (edge instanceof INDEdge) inds.add((INDEdge) edge);
            else if (edge instanceof FDEdge) fds.add((FDEdge) edge);
        }
        return setMembershipMap;
    }


    private HashMap<Edge, SetMembership> applyRules() {
        setMembershipMap = new HashMap<>();
        for (Edge edge : edges) {
            SetMembership set = applyRulesForEdge(edge);
            setMembershipMap.put(edge, set);
        }
        return setMembershipMap;
    }

    private SetMembership applyRulesForEdge(Edge edge) {
        if (edge instanceof UCCEdge) {
            return applyUCCRules((UCCEdge) edge);
        } else if (edge instanceof FDEdge) {
            return applyFDRules((FDEdge) edge);
        } else if (edge instanceof INDEdge) {
            return applyINDRules((INDEdge) edge);
        } else {
            throw new RuntimeException("Found unknown type of edge!");
        }
    }

    private SetMembership applyINDRules(INDEdge edge) {
        SetMembership membership = SetMembership.I_PLUS;
        boolean fd = cardinalityFDRule(edge);
        boolean ucc = cardinalityUCCRule(edge);
        if(fd && ucc){
            membership = SetMembership.I_MINUS;
        }
        if (!fd)
            metadata.increaseIndWithUccRule();
        if (!ucc)
            metadata.increaseIndWithFdRule();

        return membership;
    }

    private boolean cardinalityFDRule(INDEdge edge) {
        return !(isOutgoingEdgeType(edge.leftName, FDEdge.class));
    }

    private boolean cardinalityUCCRule(INDEdge edge) {
        return !(isOutgoingEdgeType(edge.leftName, UCCEdge.class));
    }

    //Out und in sind nicht richtig gesetzt für INDs
    private SetMembership applyFDRules(FDEdge edge) {
        SetMembership fd = SetMembership.F;
        boolean uccConnect = false;
        boolean exists = false;
        FDEdge broadenFDTest = edge;
        if (edge.originalEdge != null){
            if (nodes.containsKey(edge.originalEdge.leftName))
                broadenFDTest = (FDEdge) edge.originalEdge;
        }
        boolean broadenFD = false;
        if (uccBroadenFdRuleDirectly(broadenFDTest))
            uccConnect = true;
        if (uccBroadenFdRuleDirectly(edge)){
            broadenFD = true;
            fd = SetMembership.F_PLUS;
            uccConnect = true;
        }
        if (uccBroadenFdRule(edge)){
            broadenFD = true;
            fd = SetMembership.F_PLUS;
            exists = true;
        }
        if(broadenFD)
            metadata.increaseFdSubExtendingUccRule();

        if(fdSplitRule(edge)){
            metadata.increaseFdForkWithFdRule();
            fd = SetMembership.F_PLUS;
            exists = true;
        }
        if (fdChainRule(edge)){
            metadata.increaseFdChainedToFDRule();
            fd = SetMembership.F_PLUS_VALID;
            exists = true;
        }
        if (uccChainRule(edge)){
            metadata.increaseFdChainedToUccRule();
            fd = SetMembership.F_PLUS_VALID;
            exists = true;
        }
        if((!uccConnect) && exists)
            existsNodes.add(edge.leftName);

        return fd;
    }

    private boolean fdSplitRule(FDEdge edge) {
        HashSet<Edge> visited = new HashSet<>();
        visited.add(edge);
        return isOutgoingEdgeType(edge.leftName, FDEdge.class, true, visited);
    }

    private boolean uccBroadenFdRuleDirectly(FDEdge edge) {
        HashSet<Edge> visited = new HashSet<>();
        visited.add(edge);
        return isOutgoingEdgeType(edge.leftName, UCCEdge.class, false, visited);
    }

    private boolean uccBroadenFdRule(FDEdge edge) {
        HashSet<Edge> visited = new HashSet<>();
        visited.add(edge);
        return isOutgoingEdgeType(edge.leftName, UCCEdge.class, true, visited);
    }

    private SetMembership applyUCCRules(UCCEdge edge) {
        SetMembership membership = SetMembership.U;
        if(superSetUCCRule(edge)) {
            metadata.increaseUccSubExtendingUccRule();
            membership = SetMembership.U_PLUS;
        }
        if (superSetFDRule(edge)){
            metadata.increaseFdExtendingUccRule();
            membership = SetMembership.U_PLUS;
            existsNodes.add(edge.leftName);
        }
        return membership;
    }

    private boolean superSetFDRule(UCCEdge edge) {
        HashSet<Edge> visited = new HashSet<>();
        visited.add(edge);
        return isOutgoingEdgeType(edge.leftName, FDEdge.class, false, visited, true);
    }

    private boolean superSetUCCRule(UCCEdge edge) {
        HashSet<Edge> visited = new HashSet<>();
        visited.add(edge);
        return isOutgoingEdgeType(edge.leftName, UCCEdge.class, false, visited, true);
    }

    private boolean fdChainRule(Edge edge) {
        return isOutgoingEdgeType(edge.rightName, FDEdge.class);
    }

    private boolean uccChainRule(Edge edge) {
        return isOutgoingEdgeType(edge.rightName, UCCEdge.class);
    }

    //ADD visited IND Edges (only applies when graph transformation rules do not apply)
    private boolean isOutgoingEdgeType(String nodeName, Class<? extends Edge> edgeType) {
        return isOutgoingEdgeType(nodes.get(nodeName), edgeType, true, new HashSet<>(), false);
    }

    private boolean isOutgoingEdgeType(String nodeName, Class<? extends Edge> edgeType, boolean withIn) {
        return isOutgoingEdgeType(nodes.get(nodeName), edgeType, withIn, new HashSet<>(), false);
    }

    private boolean isOutgoingEdgeType(String nodeName, Class<? extends Edge> edgeType, boolean withIn, Set<Edge> visited) {
        return isOutgoingEdgeType(nodes.get(nodeName), edgeType, withIn, visited, false);
    }

    private boolean isOutgoingEdgeType(String nodeName, Class<? extends Edge> edgeType, boolean withIn, Set<Edge> visited, boolean atLeastOnIND) {
        return isOutgoingEdgeType(nodes.get(nodeName), edgeType, withIn, visited, atLeastOnIND);
    }

    private boolean isOutgoingEdgeType(Node node, Class<? extends Edge> edgeType, boolean withIn, Set<Edge> visited, boolean atLeastOnIND) {
        for (Edge outgoingEdge : node.getEdgesOutgoing()) {
            if(visited.contains(outgoingEdge)) continue;
            if ((!atLeastOnIND) && edgeType.isInstance(outgoingEdge)) return true;
            if (outgoingEdge instanceof INDEdge){
                visited.add(outgoingEdge);
                if(isOutgoingEdgeType(outgoingEdge.rightName, edgeType, withIn, visited, false)) return true;
            }
        }
        if(withIn) {
            for (Edge incomingEdge : node.getEdgesIncoming()) {
                if(visited.contains(incomingEdge)) continue;
                if (incomingEdge instanceof INDEdge){
                    visited.add(incomingEdge);
                    if(isOutgoingEdgeType(incomingEdge.leftName, edgeType, withIn, visited, false)) return true;
                }
            }
        }
        return false;
    }

    private boolean isIngoingEdgeType(String nodeName, Class<? extends Edge> edgeType) {
        return isIngoingEdgeType(nodes.get(nodeName), edgeType, true, new HashSet<>(), false);
    }

    private boolean isIngoingEdgeType(String nodeName, Class<? extends Edge> edgeType, boolean withOut) {
        return isIngoingEdgeType(nodes.get(nodeName), edgeType, withOut, new HashSet<>(), false);
    }
    private boolean isIngoingEdgeType(String nodeName, Class<? extends Edge> edgeType, boolean withOut, Set<Edge> visited, boolean atLeastOnIND) {
        return isIngoingEdgeType(nodes.get(nodeName), edgeType, withOut, visited, atLeastOnIND);
    }
    private boolean isIngoingEdgeType(Node node, Class<? extends Edge> edgeType, boolean withOut, Set<Edge> visited, boolean atLeastOnIND) {
        for (Edge ingoingEdge : node.getEdgesIncoming()) {
            if (visited.contains(ingoingEdge)) continue;
            if ((!atLeastOnIND) && edgeType.isInstance(ingoingEdge)) return true;
            if (ingoingEdge instanceof INDEdge){
                visited.add(ingoingEdge);
                if(isIngoingEdgeType(ingoingEdge.leftName, edgeType, withOut,visited, false)) return true;
            }
        }
        if (withOut){
            for (Edge outcomingEdge : node.getEdgesOutgoing()) {
                if (visited.contains(outcomingEdge)) continue;
                if (outcomingEdge instanceof INDEdge){
                    visited.add(outcomingEdge);
                    if(isIngoingEdgeType(outcomingEdge.rightName, edgeType, withOut,visited, false)) return true;
                }
            }
        }
        return false;
    }
    private List<Graph> applyGraphDecomposition() {

        // Apply FD Decomposition
        for (Edge edge : new ArrayList<>(edges)){
            if(!(edge instanceof FDEdge) || edge.isEndEdge(nodes)) continue;
            Edge edgeLHS = new FDEdge(edge.leftName, edge.leftName+" -(rhs)-> "+edge.rightName, (FDEdge) edge);
            Edge edgeRHS = new FDEdge(edge.leftName+" -(lhs)-> "+edge.rightName, edge.rightName, (FDEdge) edge);
            removeEdge2(edge);
            addEdge(edgeLHS);
            addEdge(edgeRHS);
            metadata.increaseFDDecomposition();
        }

        List<Graph> g = Graph.connectedComponents(this);

        // Apply IND Decomposition
        List<Graph> gPrime = new ArrayList<>();
        for (Graph graph : g){
            if (!graph.hasINDEdge() || graph.edges.size() == 1){
                gPrime.add(graph);
                continue;
            }
            List<List<Node>> indConnectedComponents = Graph.indConnectedComponents(graph);
            for (List<Node> indCC : indConnectedComponents){
                for (int i = 0; i < indCC.size(); i++){
                    Node x = indCC.get(i);
                    for (int j = 0; j < indCC.size(); j++){
                        Node y = indCC.get(j);
                        if (x.equals(y)) continue;
                        List<INDEdge> indPath = graph.findINDPath(x.name, y.name);
                        gPrime.add(Graph.fromINDEdges(indPath, x, y));
                        metadata.increaseINDDecomposition();
                    }
                }
            }
        }
        g = gPrime;

        // Apply Bushy Decomposition
        gPrime = new ArrayList<>();
        while(!g.isEmpty()){
            Graph graph = g.remove(0);
            boolean changed = false;
            for (String nodeName : graph.nodes.keySet()){
//                if (changed) break;
                Node node = graph.getNodes().get(nodeName);
                Set<Edge> edgesFromNode = node.edges();
                if(edgesFromNode.size() > 2){
                    changed = true;
                    int i = 0;
                    for (Edge n : edgesFromNode){
                        int j = 0;
                        for (Edge m : edgesFromNode){
                            if (i < j){
                                Graph newGraph = Graph.fromEdges(n,m, graph.nodes.get(nodeName), graph.edges);
                                g.add(newGraph);
                                metadata.increaseBushyDecomposition();
                            }
                            j++;
                        }
                        i++;
                    }
                }
            }
            if(!changed) gPrime.add(graph);
        }

        g = gPrime;
        return g;
    }

    private boolean nonEmpty(List<INDEdge> indPath, Node x, Node y) {
        for (INDEdge edge : indPath){
            if ((!nodes.get(edge.leftName).equals(x))
                    && (!nodes.get(edge.rightName).equals(x))
                    && (nodes.get(edge.leftName).edges().size() > 2
                    || nodes.get(edge.rightName).edges().size() > 2))
                return true;
        }
        return false;
    }

    private List<INDEdge> findINDPath(String startNodeName, String endNodeName) {
        List<String> nodesInPath = indBFS(startNodeName, endNodeName);
        List<INDEdge> edges = new ArrayList<>();
        for (int i = 0; i < nodesInPath.size()-1; i++){
            edges.add(nodes.get(nodesInPath.get(i)).indEdge(nodesInPath.get(i+1)));
        }
        if (edges.isEmpty())
            throw new RuntimeException("No IND Path found!");
        return edges;
    }


    private boolean hasINDEdge() {
        for (Edge edge : edges)
            if (edge instanceof INDEdge) return true;
        return false;
    }

    private List<Graph> applyGraphTransformation() {

        //Apply FD Transformation
        for (Edge edge : new ArrayList<>(edges)){
            if(!(edge instanceof FDEdge)) continue;
            //if(edge.isEndEdge(nodes)) continue;
            Edge edgeLHS = new FDEdge(edge.leftName, edge.rightName+"RHS", (FDEdge) edge);
            Edge edgeRHS = new FDEdge(edge.leftName+"LHS", edge.rightName, (FDEdge) edge);
            removeEdge(edge);
            addEdge(edgeLHS);
            addEdge(edgeRHS);
            metadata.increaseFDDecomposition();
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
        if(!newRestGraph.edges.isEmpty()) {
            graphs.add(newRestGraph);
            metadata.increaseBushyDecomposition();
        }

        //Apply IND Transformation
        Set<INDEdge> edgesAlreadyChanged = new HashSet<>();
        for (Edge edge : edges){
            if(!(edge instanceof INDEdge)) continue;
            if(edgesAlreadyChanged.contains(edge)) continue;
            Set<INDEdge> set = allConnectedEdges((INDEdge) edge);
            edgesAlreadyChanged.addAll(set);
            Map<String, List<String>> graph = buildGraph(set);
            for(Pair<String,String> pair : generateAllPairs(getAllNodesFromEdges(set))){
                List<INDEdge> newEdges = findRelationEdges(graph, pair.first(), pair.second());
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
                    metadata.increaseINDDecomposition();
                }
            }
        }
        return graphs;
    }

    public void removeNode(String newNodeName) {
        removeNode(getNode(newNodeName));
    }
    public void removeNode(Node newNodeName) {
        if(newNodeName != null){
            if (newNodeName.isEmpty())
                nodes.remove(newNodeName.getName());
            else
                throw new RuntimeException("Node cant be deleted if not emptied first!");
        }
    }

    public Graph copy() {
        Graph copy = new Graph();
        copy.edges = new HashSet<>(this.edges);
        for(String nodeName : nodes.keySet()){
            copy.nodes.put(nodeName, nodes.get(nodeName).copy(nodeName));
        }
        return copy;
    }

    public Graph copyMap(List<String> permutation) {
        Graph copy = new Graph();
        int i = 0;
        HashMap<String, String> mapping = new HashMap<>();
        for (String nodeName : getNodes().keySet()){
            copy.nodes.put(permutation.get(i), getNode(nodeName).copy(permutation.get(i)));
            mapping.put(nodeName, permutation.get(i));
            i++;
        }
        for (Edge edge : getEdges()){
            copy.edges.add(edge.copy(mapping));
        }
        return copy;
    }

    public List<ResultNode> getResultNodes(Edge min) {
        List<ResultNode> result = new ArrayList<>();
        ResultNode lhsNode = getResultNode(min.leftName);
        result.add(lhsNode);
        if (!(min instanceof UCCEdge)){
            ResultNode rhsNode = getResultNode(min.rightName);
            result.add(rhsNode);
        }
        return result;
    }

    public ResultNode getResultNode(String name) {
        ResultNode lhsNode = new ResultNode(name);
        for (Edge edge : getEdges(name)){
            lhsNode.add(edge);
        }
        return lhsNode;
    }

    public List<Triple<Boolean, Edge, Boolean>> getNeighborsWithDir(Edge min) {
        List<Triple<Boolean, Edge, Boolean>> neighbors = new ArrayList<>();
        for (Edge edge : getEdges(min.leftName)){
            if (min.equals(edge)) continue;
            Triple<Boolean, Edge, Boolean> triple = new Triple<>(Boolean.TRUE, edge, edge.leftName.equals(min.leftName));
            neighbors.add(triple);
        }
        if (!(min instanceof UCCEdge)){
            for (Edge edge : getEdges(min.rightName)){
                if (min.equals(edge)) continue;
                Triple<Boolean, Edge, Boolean> triple = new Triple<>(Boolean.FALSE, edge, edge.leftName.equals(min.rightName));
                neighbors.add(triple);
            }
        }
        return neighbors;
    }

    private List<Edge> getEdges(String name) {
        List<Edge> result = new ArrayList<>();
        for (Edge edge : edges){
            if (edge.contains(name))
                result.add(edge);
        }
        return result;
    }

    public List<List<Edge>> getClustersInBFS() {
        List<List<Edge>> cluster = new ArrayList<>();
        Set<String> nodesVisited = new HashSet<>(nodes.keySet());
        while (!nodesVisited.isEmpty()){
            List<Edge> edgesOrderList = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Queue<Node> queue = new LinkedList<>();

            Node startNode = nodes.get(nodesVisited.stream().findFirst().get());
            queue.add(startNode);
            visited.add(startNode.name);
            while (!queue.isEmpty()) {
                Node current = queue.poll();

                for (Edge edge : current.getEdges()) {
                    // Add the edge to the HashSet
                    if (edgesOrderList.contains(edge))
                        continue;
                    edgesOrderList.add(edge);

                    String neighborId = edge.getOtherNeighbor(current.name);
                    // If the neighbor hasn't been visited, add it to the queue
                    if (!visited.contains(neighborId)) {
                        visited.add(neighborId);
                        Node neighborNode = nodes.get(neighborId);
                        if (neighborNode != null) {
                            queue.add(neighborNode);
                        }
                    }
                }
            }
            nodesVisited.removeAll(visited);
            cluster.add(edgesOrderList);
        }
        return cluster;
    }


    public List<Edge> getEdgesInBFS() {
        List<Edge> edgesOrderList = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();

        Node startNode = nodes.values().stream().findFirst().get();
        queue.add(startNode);
        visited.add(startNode.name);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            for (Edge edge : current.getEdges()) {
                // Add the edge to the HashSet
                if (edgesOrderList.contains(edge))
                    continue;
                edgesOrderList.add(edge);

                String neighborId = edge.getOtherNeighbor(startNode.name);
                // If the neighbor hasn't been visited, add it to the queue
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    Node neighborNode = nodes.get(neighborId);
                    if (neighborNode != null) {
                        queue.add(neighborNode);
                    }
                }
            }
        }
        return edgesOrderList;
    }

    public HashSet<Edge> getEdges() {
        return edges;
    }

    public HashMap<String, Node> getNodes() {
        return nodes;
    }

    public HashMap<Edge, SetMembership> getSetMembershipMap() {
        return setMembershipMap;
    }

    public Set<String> getExistsNodes() {
        return existsNodes;
    }

    public GraphMetadata getMetadata() {
        return metadata;
    }

    public static HashMap<GraphAtom, List<GraphAtom>> getDecomposedGraphMap() {
        return decomposedGraphMap;
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
            lastName = nodeName;
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

        throw new RuntimeException("No connection found between two nodes!");
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
            //if (edge.isEndEdge(nodes)) continue;
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

    public void removeEdge2(Edge edge){
        edges.remove(edge);
        nodes.get(edge.rightName).removeEdge(edge);
        nodes.get(edge.leftName).removeEdge(edge);
        if (nodes.get(edge.rightName).isEmpty()) nodes.remove(edge.rightName);
        if (nodes.get(edge.leftName).isEmpty()) nodes.remove(edge.leftName);
    }

    public void removeEdge(Edge edge){
        edges.remove(edge);
    }
    public boolean hasEdge(Edge edge){
        return edges.contains(edge);
    }
    public void addEdge(Edge edge){
        if (edges.contains(edge)) return;
        addNode(edge.leftName);
        addNode(edge.rightName);
        Node left = nodes.get(edge.leftName);
        Node right = nodes.get(edge.rightName);
        left.addEdge(edge);
        right.addEdge(edge);
        if(!hasEdge(edge))
            edges.add(edge);
    }

    public Node getNode(String nodeName) {
        return nodes.get(nodeName);
    }

    public void addNode(String nodeName) {
        if (!nodes.containsKey(nodeName)) {
            nodes.put(nodeName, new Node(nodeName));
        }
    }

    public Collection<Edge> getMinimalEdges(){
        List<Edge> result = new ArrayList<>();
        for (Edge edge : setMembershipMap.keySet()){
            if(SetMembership.isMinimal(setMembershipMap.get(edge)))
                result.add(edge);
        }
        return result;
    }

    void dfs(String v, Set<String> visited) {
        visited.add(v);

        for (String x : nodes.get(v).neighbors()) {
            if (!visited.contains(x))
                dfs(x, visited);
        }
    }

    List<String> indBFS(String startNode, String endNode) {
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> parentMap = new HashMap<>();
        queue.add(startNode);
        visited.add(startNode);
        parentMap.put(startNode, null);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (String neighbor : nodes.get(current).indNeighbors()) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    parentMap.put(neighbor, current);
                    if (neighbor.equals(endNode)){
                        return reconstructPath(parentMap, endNode);
                    }
                    visited.add(neighbor);
                }
            }
        }
        throw new RuntimeException("Path does not exist!");
    }

    void indDFS(String v, Set<String> visited) {
        visited.add(v);

        for (String x : nodes.get(v).indNeighbors()) {
            if (!visited.contains(x))
                indDFS(x, visited);
        }
    }

    public static List<List<Node>> indConnectedComponents(Graph graph){
        List<List<Node>> indCCs = new ArrayList<>();
        Set<String> allVisited = new HashSet<>();
        for (String nodeName : graph.getNodes().keySet()) {
            if(graph.nodes.get(nodeName).indNeighbors().isEmpty()) continue;
            if (!allVisited.contains(nodeName)) {
                Set<String> visited = new HashSet<>();
                graph.indDFS(nodeName, visited);
                indCCs.add(visited.stream().map(graph::getNode).collect(Collectors.toList()));
                allVisited.addAll(visited);
            }
        }
        return indCCs;
    }

    public static List<Graph> connectedComponents(Graph graph){
        // Mark all the vertices as not visited
        List<Graph> graphs = new ArrayList<>();
        Set<String> allVisited = new HashSet<>();
        for (String nodeName : graph.getNodes().keySet()) {
            if (!allVisited.contains(nodeName)) {
                Set<String> visited = new HashSet<>();
                graph.dfs(nodeName, visited);
                if(visited.size() == graph.getNodes().size()){
                    graphs.add(graph);
                    break;
                } else {
                    graphs.add(Graph.fromNodes(graph.getNodesFromString(visited)));
                }
                allVisited.addAll(visited);
            }
        }
        return graphs;
    }

    private static Graph fromINDEdges(List<INDEdge> indPath, Node x, Node y) {
        Graph graph = new Graph();
        for (INDEdge edge : indPath) graph.addEdge(edge);
        for (Edge edge : x.edges()) {
            if (edge instanceof INDEdge) continue;
            graph.addEdge(edge);
        }
        for (Edge edge : y.edges()) {
            if (edge instanceof INDEdge) continue;
            graph.addEdge(edge);
        }
        return graph;
    }

    private static Graph fromEdges(Edge n, Edge m, Node node, HashSet<Edge> edges) {
        Graph graph = new Graph();
        graph.addNode(node.name);
        graph.addEdge(n);
        graph.addEdge(m);
        if (n instanceof INDEdge || m instanceof INDEdge){
            for (Edge edge : edges){
                if (edge instanceof INDEdge) graph.addEdge(edge);
            }
            for (Edge edge : edges){
                if ((!edge.rightName.equals(node.name))
                        && (!edge.leftName.equals(node.name))
                        && (graph.nodes.containsKey(edge.leftName)
                        || graph.nodes.containsKey(edge.rightName))
                )
                    graph.addEdge(edge);
            }
        }
        return graph;
    }

    private static Graph fromNodes(Collection<Node> nodeCollection) {
        Graph graph = new Graph();
        for (Node node : nodeCollection){
            graph.addNode(node.name);
            for (Edge edge : node.getEdgesIncoming()){
                graph.addEdge(edge);
            }
            for (Edge edge : node.getEdgesOutgoing()){
                graph.addEdge(edge);
            }
        }
        return graph;
    }

    private Collection<Node> getNodesFromString(Set<String> visited) {
        Collection<Node> result = new ArrayList<>();
        for (String nodeName : visited) {
            if (nodes.containsKey(nodeName)) {
                result.add(nodes.get(nodeName));
            }
        }
        return result;
    }


    public enum SetMembership {
        F(0),
        F_PLUS(1),
        F_PLUS_VALID(2),
        U(3),
        U_PLUS(4),
        I(5),
        I_MINUS(6),
        I_PLUS(7);

        private final int value;

        SetMembership(int value) {
            this.value = value;
        }

        public static SetMembership getMembership(Edge edge){
            if(edge instanceof INDEdge)
                return I_MINUS;
            if(edge instanceof FDEdge)
                return F;
            if(edge instanceof UCCEdge)
                return U;
            return null;
        }

        public int getValue() {
            return value;
        }

        public static boolean isMinimal(SetMembership set){
            return set == F || set == U || set == I_MINUS;
        }

        public static String toString(Edge edge, Set<String> set) {
            String left = edge.leftName;
            String right = edge.rightName;
            if (set.contains(left))
                left = left + "'";
            if (set.contains(right))
                right = right + "'";
            String arrow = left + " \\rightarrow " + right;
            String subset = left + " \\subseteq " + right;

            if (edge instanceof FDEdge){
                return "("+ arrow + ")";
            } else if (edge instanceof UCCEdge){
                return "(" + left + ")";
            } else if (edge instanceof INDEdge){
                return "(" + subset + ")";
            } else {
                throw new RuntimeException("Unkown Edge: " + edge);
            }
        }

        public static String toString(SetMembership membership, Edge edge) {
            String left = edge.leftName;
            String right = edge.rightName;
            String arrow = left + " \\rightarrow " + right;
            String subset = left + " \\subseteq " + right;

            switch (membership) {
                case F:
                    return "("+ arrow + ") \\in F";
                case F_PLUS:
                    return "("+ arrow + ") \\in F^+";
                case F_PLUS_VALID:
                    return "valid(("+ arrow + "))";
                case U:
                    return "(" + left + ") \\in U";
                case U_PLUS:
                    return "(" + left + ") \\in U^+";
                case I:
                    return "("+ subset + ") \\in I";
                case I_PLUS:
                    return "("+ subset + ") \\in I^+";
                case I_MINUS:
                    return "(" + subset + ") \\in I^+\\land|" + left + "| = 1\\land|" + right + "| = 1";
                default:
                    return "";
            }
        }
    }
    public static Graph fromConditions(List<Condition> conditions){
        Graph graph = new Graph();
        for (Condition condition : conditions){
            if (condition == null) continue;
                //throw new RuntimeException("At least one condition was not correctly parsed!");
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
                    if (uccCondition.not) continue;
                    String[] arrayUCC = uccCondition.ccFunction.toArray(new String[uccCondition.ccFunction.size()]);
                    graph.addEdge(new UCCEdge(uccCondition.id,arrayUCC));
                    break;
                default:
                    break;
            }
        }
        return graph;
    }

    @Override
    public String toString() {
        return edges.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Graph graph = (Graph) o;
        return Objects.equals(edges, graph.edges) && Objects.equals(nodes, graph.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edges, nodes);
    }

}
