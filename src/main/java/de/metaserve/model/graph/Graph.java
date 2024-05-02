package de.metaserve.model.graph;


import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.results.FunctionalDependency;
import de.metanome.algorithm_integration.results.InclusionDependency;
import de.metanome.algorithm_integration.results.Result;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.constraints.Pair;
import de.metaserve.model.query.Query;
import de.metaserve.model.result.ResultSet;

import java.util.*;
import java.util.function.Function;

public class Graph {
    public Map<String, Node> nodes = new HashMap<>();

    public void add(String name){
        if(!nodes.containsKey(name))
            nodes.put(name, new Node(name));
    }

    public void add(String name, List<Result> resultList, boolean negate){
        add(name);
        Set<ColumnCombination> ccs = new HashSet<>();
        for (Result result : resultList){
            ColumnCombination ids = ((UniqueColumnCombination) result).getColumnCombination();
            nodes.get(name).add(ids, negate);
            ccs.add(ids);
        }
        NewEdge newEdge = Edge.getSubType(negate, "UCC");
        if(negate) {
            ((NegativeConstraint) newEdge).addAll(ccs);
        } else {
            ((UCCEdge)newEdge).addAll(ccs);
        }

        nodes.get(name).addNewEdge(newEdge);
    }

    public void add(String name, List<Result> resultList){
        add(name);
        UCCEdge newEdge = (UCCEdge) Edge.getSubType(false,"UCC");
        for (Result result : resultList){
            ColumnCombination ids = ((UniqueColumnCombination) result).getColumnCombination();
            nodes.get(name).add(ids);
            newEdge.add(ids);
        }
        nodes.get(name).addNewEdge(newEdge);
    }

    public void add(String left, String right, List<Result> resultList){
        add(left);
        add(right);
        for (Result result : resultList){
            if(result instanceof FunctionalDependency) {
                ColumnCombination leftID = ((FunctionalDependency) result).getDeterminant();
                ColumnIdentifier rightID = ((FunctionalDependency) result).getDependant();
                addEdge("FD", left, right, leftID, convertIDtoComb(rightID));
            } else if(result instanceof InclusionDependency) {
                ColumnPermutation leftID = ((InclusionDependency) result).getDependant();
                ColumnPermutation rightID = ((InclusionDependency) result).getReferenced();
                addEdge("IND", left, right, convertPremutationToComb(leftID), convertPremutationToComb(rightID));
            }
        }
    }

    private ColumnCombination convertPremutationToComb(ColumnPermutation leftID) {
        ColumnCombination columnCombination = new ColumnCombination();
        columnCombination.setColumnIdentifiers(new HashSet<>(leftID.getColumnIdentifiers()));
        return columnCombination;
    }

    private ColumnCombination convertIDtoComb(ColumnIdentifier leftID) {
        return new ColumnCombination(leftID);
    }

    private void addEdge(String type, String left, String right, ColumnCombination leftCC, ColumnCombination rightCC) {
        Node leftNode = nodes.get(left);
        Node rightNode = nodes.get(right);
        Edge edge;
        NewEdge newEdge;
        if(leftNode.hasEdge(rightNode, type)){
            edge = leftNode.getEdge(rightNode, type);
            newEdge = leftNode.getNewEdge(rightNode, type);
        } else {
            newEdge = Edge.getSubType(false, type);
            edge = new Edge(type, leftNode, rightNode);
            leftNode.addEdge(edge);
            rightNode.addEdge(edge);
            newEdge.left = leftNode;
            newEdge.right = rightNode;
            leftNode.addNewEdge(newEdge);
            rightNode.addNewEdge(newEdge);
        }
        newEdge.add(leftCC, rightCC);
        edge.add(leftCC, rightCC);
    }

    public List<ResultSet> getResults(Query query, List<String> selections) {
        computeResults(selections);
        query.getMetaData().update(QueryEngine.QueryState.COMPUTED_MIN);
        ResultSet joinTable = joinTables(selections);
        ResultSet filterTable = filter(joinTable, selections);
        //Map<String, List<String>> resultMap = retrieveResults(selections);
        //Map<String, List<String>> tableMap = retrieveTables(selections);
        //return formatResults(resultMap, tableMap);
        //return new ArrayList<>();
        return Arrays.asList(filterTable);
    }

    private ResultSet filter(ResultSet joinTable, List<String> selections) {
        joinTable.select(selections);
        Map<String, AggregateFunction> functions = new HashMap<>();
        functions.put("Y", AggregateFunction.SIZE);
        joinTable.groupBy(Arrays.asList("X"), functions);
        joinTable.orderBy(Arrays.asList("X"));
        return joinTable;
    }

    private ResultSet joinTables(List<String> selections) {
        Set<ResultSet> tables = getAllTables();
        Queue<ResultSet> tableQueue = new ArrayDeque<>(tables);
        ResultSet start = tableQueue.poll();
        if(start == null)
            //throw new RuntimeException("Unexpected join error!");
            return null;
        while (!tableQueue.isEmpty()){
            ResultSet joinCanidate = tableQueue.poll();
            if(start.canJoin(joinCanidate)){
                start = start.multiJoin(joinCanidate);
            } else {
                tableQueue.add(joinCanidate);
            }
        }
/*
        start.getRows().removeIf(row ->
                row.get(0).equalsIgnoreCase(row.get(2)) ||
                row.get(0).equalsIgnoreCase(row.get(1)) ||
                row.get(0).equalsIgnoreCase(row.get(3)) ||
                row.get(1).equalsIgnoreCase(row.get(3)) ||
                row.get(1).equalsIgnoreCase(row.get(2)) ||
                row.get(2).equalsIgnoreCase(row.get(3))
        );


        System.out.println(start.getColumnNames());
        for (List<String> row : start.getRows()){
            System.out.println(row);
        }
        System.out.println(start.size());

 */
        return start;
    }

    private Set<ResultSet> getAllTables() {
        Set<ResultSet> tables = new HashSet<>();
        for (Node node : nodes.values()){
            tables.addAll(node.getNewTables());
        }
        return tables;
    }

    private Map<String, List<String>> retrieveTables(List<String> selections) {
        Map<String, List<String>> resultMap = new HashMap<>();
        for (String setName : selections){
            Node node = nodes.get(setName);
            boolean found = false;
            for (String mapName : resultMap.keySet()){
                Node nodeToFind = nodes.get(resultMap.get(mapName).get(0));
                if(findNode(node, nodeToFind)){
                    found = true;
                    resultMap.get(mapName).add(setName);
                    break;
                }
            }
            if(!found){
                List<String> tempList = new ArrayList<>();
                tempList.add(setName);
                resultMap.put(setName, tempList);
            }
        }
        return resultMap;
    }

    private List<ResultSet> formatResults(Map<String, List<String>> resultMap, Map<String, List<String>> tableMap) {
        List<ResultSet> formattedResult = new ArrayList<>();
        for(String tableName: tableMap.keySet()){
            List<String> setNamesOfTable = tableMap.get(tableName);
            //validateResults(setNamesOfTable, resultMap);
            ResultSet resultSet = new ResultSet(setNamesOfTable);
            for (int i = 0; i < resultMap.get(setNamesOfTable.get(0)).size(); i++) {
                List<String> row = new ArrayList<>();
                for (String setName : setNamesOfTable){
                    row.add(resultMap.get(setName).get(i));
                }
                resultSet.add(row);
            }
            formattedResult.add(resultSet);
        }
        return formattedResult;
    }

    private void validateResults(List<String> setNamesOfTable, Map<String, List<String>> resultMap) {
        int commonSize = resultMap.get(setNamesOfTable.get(0)).size();
        for (int i = 1; i < setNamesOfTable.size(); i++) {
            int size = resultMap.get(setNamesOfTable.get(i)).size();
            if(commonSize != size)
                throw new RuntimeException("Result sizes do not match!");
        }
    }

    private Map<String, List<String>> retrieveResults(List<String> selections) {
        Map<String, List<String>> resultMap = new HashMap<>();

        for (String setName : selections){
            resultMap.put(setName, nodes.get(setName).getNewResults());
        }
        return resultMap;
    }

    private void computeResults(List<String> selections) {
        /*
        for (String setName : selections){
            Node node = nodes.get(setName);

            if(node == null)
                throw new RuntimeException("Set " + setName + " is not defined!");
            if(node.isFinished())
                continue;
            Node endNode;
            if(node.isEndNode())
                endNode = node;
            else
                endNode = findEndNode(node);
            //computeGlobalMinimum(endNode);
        }
         */
        for (Node node: nodes.values()){
            node.initNew();
        }
        for (int i = 0; i < 1; i++) {
            for (Node node: nodes.values()){
                node.computeLocaleMin();
            }

        }

/*
        List<NewEdge> list = new ArrayList<>();
        for (Node node: nodes.values()){

            for(NewEdge edge :  node.newEdges){
                if(edge instanceof INDEdge){
                    list.add(edge);
                } else if (edge instanceof FDEdge){
                    list.add(edge);
                }
            }
        }

        checkCycle(list.get(0), list.get(1));

 */
    }

    private void checkCycle(NewEdge newEdge, NewEdge newEdge1) {
        Iterator<Map.Entry<ColumnCombination, List<ColumnCombination>>> iterator = newEdge.allEdges.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ColumnCombination, List<ColumnCombination>> entry = iterator.next();
            ColumnCombination ccLeft = entry.getKey();
            List<ColumnCombination> allRightSides = entry.getValue();

            Iterator<ColumnCombination> ccRightIterator = allRightSides.iterator();
            while (ccRightIterator.hasNext()) {
                ColumnCombination ccRight = ccRightIterator.next();

                if (newEdge1.allNewEdges.containsKey(ccRight)) {
                    List<ColumnCombination> allRightSides1 = newEdge1.allEdges.get(ccRight);
                    if (!allRightSides1.contains(ccLeft)) {
                        ccRightIterator.remove();
                    }
                } else {
                    ccRightIterator.remove();
                }
            }

            if (allRightSides.isEmpty()) {
                iterator.remove();
                newEdge.allNewEdges.remove(ccLeft);
            }
        }
    }

    private boolean findNode(Node startNode, Node endNode) {
        return findNode(startNode, endNode, new HashSet<>());
    }

    private boolean findNode(Node node, Node endNode, Set<Node> visited){
        if(node.equals(endNode))
            return true;
        for (Node connectedNode : node.getNeighborNodes()){
            if(visited.contains(connectedNode))
                continue;
            visited.add(node);
            if (findNode(connectedNode, endNode, visited))
                return true;
        }
        return false;
    }

    private Node findEndNode(Node node) {
        return findEndNode(node, new HashSet<>());
    }

    private Node findEndNode(Node node, Set<Node> visited){
        if(node.isEndNode())
            return node;
        for (Node connectedNode : node.getNeighborNodes()){
            if(visited.contains(connectedNode))
                continue;
            visited.add(node);
            Node foundNode = findEndNode(connectedNode, visited);
            if(foundNode != null)
                return foundNode;
        }
        return null;
    }

    private void computeGlobalMinimum(Node startNode) {
        Node currentNode;
        Stack<Node> stack = new Stack<>();
        stack.push(startNode);
        Set<Node> allNodes = new HashSet<>();
        allNodes.add(startNode);

        while(!stack.isEmpty()){
            currentNode = stack.pop();
            currentNode.getLocalMinimum();

            for (Node n : currentNode.getNeighborNodes()) {
                if (!allNodes.contains(n)){
                    stack.add(n);
                    allNodes.add(n);
                }

            }
            //List<Node> effectNeighbors = currentNode.getLocalMinimum();
            //stack.addAll(effectNeighbors);
        }

        stack.push(startNode);
        allNodes.remove(startNode);

        while(!stack.isEmpty()){
            currentNode = stack.pop();

            List<Node> neighbors = currentNode.test();
            stack.addAll(neighbors);
            allNodes.removeAll(neighbors);

            /*
            if(stack.isEmpty() && !allNodes.isEmpty())
                stack.addAll(allNodes);
               // @TODO Was war hiermit gemeint???
             */
        }

        //@TODO mark all nodes in this cluster as finished
        for (Node node : allNodes) {
            node.finished = true;
        }
    }


}
