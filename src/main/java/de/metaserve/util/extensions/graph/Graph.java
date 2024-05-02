package de.metaserve.util.extensions.graph;


import de.metaserve.model.result.ResultSet;

import java.util.*;

public class Graph {
    public HashMap<String, Node> map = new HashMap<>();
    PriorityQueue<Edge> edges = new PriorityQueue<>();

    ResultSet resultSet;

    public Graph(ResultSet input){
        resultSet = new ResultSet(input.getColumnNames());
        importResultSet(input);
        optimize();
        while(!map.isEmpty()){
            Edge topEdge = edges.poll();
            addResult(topEdge.left, topEdge.right);
            if(topEdge.left.isEmpty())
                removeNode(topEdge.left);
            if(topEdge.right.isEmpty())
                removeNode(topEdge.right);
            optimize();
        }
    }

    private void importResultSet(ResultSet input) {
        for(List<String> row : input.getRows()){
            addEdge(row.get(0).replace("[", "").replace("]",""), row.get(1).replace("[", "").replace("]",""));
        }
    }

    private void addNode(String name){
        map.put(name, new Node(name));
    }

    public void addEdge(String from, String to) {
        if (!map.containsKey(from)) {
            addNode(from);
        }
        if (!map.containsKey(to)) {
            addNode(to);
        }
        Edge edge = new Edge(map.get(from), map.get(to));
        edges.add(edge);
        map.get(from).addOutEdge(edge);
        map.get(to).addInEdge(edge);
    }

    public void removeNode(Node node){
        if(node.hasNeighbors()){
            for(Edge out : node.getOut()){
                stack.add(out.right.name);
                edges.remove(out);
            }

            for(Edge in : node.getIn()){
                stack.add(in.left.name);
                edges.remove(in);
            }
        }
        map.remove(node.name);
    }

    public void removeEdge(Edge edge){
        removeEdge(edge.left, edge.right);
    }

    public void removeEdge(Node from, Node to){
        stack.add(from.name);
        stack.add(to.name);
        edges.remove(from.getEdge(to));
        from.removeOutEdge(to);
        to.removeInEdge(from);
        if(from.isEmpty())
            removeNode(from);
        if(to.isEmpty())
            removeNode(to);
    }

    Queue<String> stack = new ArrayDeque<>();
    public void optimize(){
        stack.clear();
        stack.addAll(map.keySet());
        while(!stack.isEmpty()){
            String name = stack.poll();
            Node tmp = map.get(name);
            if(tmp == null)
                continue;
            if(tmp.isEmpty())
                removeNode(tmp);
            if(tmp.isEndNode() && tmp.inSize() == 1){
                if(isNodeAllowedToMatch(tmp)){
                    addResult(tmp.getIn().get(0).left, tmp);
                    removeNode(tmp);
                }
            } else if(tmp.isStartNode() && tmp.outSize() == 1){
                if(isNodeAllowedToMatch(tmp)){
                    addResult(tmp,tmp.getOut().get(0).right);
                    removeNode(tmp);
                }
            } else if(tmp.inSize() == 1){
                if(isNodeAllowedToMatch(tmp)){
                    addResult(tmp.getIn().get(0).left, tmp);
                }
            } else if(tmp.outSize() == 1){
                if(isNodeAllowedToMatch(tmp)) {
                    addResult(tmp, tmp.getOut().get(0).right);
                }
            }
        }
    }

    private boolean isNodeAllowedToMatch(Node tmp) {
        if(tmp.isEndNode()){

        } else if(tmp.isStartNode()){

        }
        else if(tmp.inSize() == 1){

        }
        else if(tmp.outSize() == 1){

        }
        return true;
    }

    private void addResult(Node left, Node right) {
        for (Edge out : left.getOut()){
            removeEdge(out);
        }

        for (Edge out : right.getIn()){
            removeEdge(out);
        }

        List<String> row = new ArrayList<>();
        row.add(left.name);
        row.add(right.name);
        resultSet.add(row);
    }

    public ResultSet getResult(){
        if(!map.isEmpty())
            System.out.println(map.size() + " not matched!");
        return resultSet;
    }
}
