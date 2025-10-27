package de.metaserve.executor;

import de.metanome.Metanome;
import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.results.FunctionalDependency;
import de.metanome.algorithm_integration.results.InclusionDependency;
import de.metanome.algorithm_integration.results.Result;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.constraints.Condition;
import de.metaserve.model.constraints.FD;
import de.metaserve.model.constraints.IND;
import de.metaserve.model.constraints.UCC;
import de.metaserve.model.graph.Graph;
import de.metaserve.model.out.Console;
import de.metaserve.parser.query.Query;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.util.*;
import java.util.function.Function;

public class DefaultExecutor implements Executor {
    private final ExecutorConfiguration configuration;

    public DefaultExecutor(ExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<ResultSet> executeQuery(Query query) {
        Metanome metanome = Metanome.getInstance();
        Graph graph = buildGraph(metanome, query);
        query.getMetaData().update(QueryEngine.QueryState.QUERY_WAITING_FOR_METANOME);
        List<ResultSet> results = graph.getResults(query, query.getSelections());

        if(!configuration.getOutputType().equals(ExecutorConfiguration.Output.DEFAULT)){
            Console.get().print(graph, query.getSelections());
        }
        query.getMetaData().update(QueryEngine.QueryState.QUERY_RESULT);
        return results;
    }

    private Graph buildGraph(Metanome metanome, Query query) {
        Graph graph = new Graph();
        long startTime = 0;
        for (Condition con : query.getConditions()){
            if(con == null)
                continue;
            switch (con.getName()){
                case IND.NAME:
                    IND ind = ((IND) con);
                    Set<String> leftRightIND = new HashSet<>();
                    leftRightIND.addAll(ind.left);
                    leftRightIND.addAll(ind.right);
                    String[] arrayIND = leftRightIND.toArray(new String[leftRightIND.size()]);
                    startTime = System.currentTimeMillis();
                    List<Result> resultIND = metanome.executeIND(arrayIND);
                    System.out.println("IND:" + (System.currentTimeMillis() - startTime));
                    //convertINDResults(resultIND);
                    query.getMetaData().addStat("IND", resultIND.size());
                    verifyRightTable(resultIND, ind.left, ind.right);
                    graph.add(ind.leftName, ind.rightName, resultIND);
                    break;
                case FD.NAME:
                    FD fd = ((FD) con);
                    Set<String> leftRightFD = new HashSet<>();
                    leftRightFD.addAll(fd.left);
                    leftRightFD.addAll(fd.right);
                    String[] arrayFD = leftRightFD.toArray(new String[leftRightFD.size()]);
                    startTime = System.currentTimeMillis();
                    List<Result> resultFD = metanome.executeFD(arrayFD);
                    System.out.println("FD:" + (System.currentTimeMillis() - startTime));
                    query.getMetaData().addStat("FD", resultFD.size());
                    verifyRightTable(resultFD, fd.left, fd.right);
                    graph.add(fd.leftName, fd.rightName, resultFD);
                    break;
                case UCC.NAME:
                    UCC ucc = ((UCC) con);
                    String[] arrayUCC = ucc.ccFunction.toArray(new String[ucc.ccFunction.size()]);
                    startTime = System.currentTimeMillis();
                    List<Result> resultUCC = metanome.executeUCC(arrayUCC);
                    System.out.println("UCC:" + (System.currentTimeMillis() - startTime));
                    query.getMetaData().addStat("UCC", resultUCC.size());
                    verifyCard(resultUCC);
                    graph.add(ucc.id, resultUCC, ucc.not);
                    break;
                default:
                    break;
            }
        }
        return graph;
    }

    private void convertINDResults(List<Result> resultIND) {
        HashMap<ColumnIdentifier, ColumnCombination> indMap = new HashMap<>();
        for (Result result : resultIND) {
            InclusionDependency parsed = (InclusionDependency) result;
            if(parsed.getReferenced().getColumnIdentifiers().get(0).getTableIdentifier().equals(parsed.getDependant().getColumnIdentifiers().get(0).getTableIdentifier()))
                continue;
            if(!parsed.getReferenced().getColumnIdentifiers().get(0).getTableIdentifier().equals("source.csv"))
                continue;
            if(!parsed.getDependant().getColumnIdentifiers().get(0).getTableIdentifier().equals("target.csv"))
                continue;
            for (int i = 0; i < parsed.getReferenced().getColumnIdentifiers().size(); i++) {
                if(!indMap.containsKey(parsed.getReferenced().getColumnIdentifiers().get(i))){
                    indMap.put(parsed.getReferenced().getColumnIdentifiers().get(i), new ColumnCombination());
                }
                indMap.get(parsed.getReferenced().getColumnIdentifiers().get(i)).getColumnIdentifiers().add(parsed.getDependant().getColumnIdentifiers().get(i));
            }
        }
        for (ColumnIdentifier id : indMap.keySet()) {
            Long cardLeft = InputConfiguration.cardMap.get(id.toString());
            Long cardRightMax = 0L;
            ColumnIdentifier rightMax = null;
            for (ColumnIdentifier right : indMap.get(id).getColumnIdentifiers()) {
                Long cardRight = InputConfiguration.cardMap.get(id.toString());
                if(cardRight > cardRightMax){
                    cardRightMax = cardRight;
                    rightMax = right;
                } else if(cardRight.equals(cardLeft)){
                    System.out.println(1 + " " + id + " : " + right);
                }
            }
            System.out.println((cardLeft/cardRightMax) + " " + id + " : " + rightMax);
        }
    }


    private void verifyCard(List<Result> results) {
        if(InputConfiguration.cardMap.isEmpty())
            return;
        List<Result> toRemove = new ArrayList<>();
        for (Result result : results) {
            if (result instanceof UniqueColumnCombination) {
                for(ColumnIdentifier id : ((UniqueColumnCombination) result).getColumnCombination().getColumnIdentifiers()){
                    if(!isCardValid(id)) {
                        toRemove.add(result);
                        break;
                    }
                }
            }
        }
        for (Result result : toRemove) {
            results.remove(result);
        }
    }

    private void verifyRightTable(List<Result> results, List<String> left, List<String> right) {
        List<Result> toRemove = new ArrayList<>();
        for (Result result : results) {
            if(result instanceof FunctionalDependency) {
                ColumnCombination leftID = ((FunctionalDependency) result).getDeterminant();
                if(!isDepValid(left, leftID.getColumnIdentifiers())){
                    toRemove.add(result);
                    continue;
                }
                ColumnIdentifier rightID = ((FunctionalDependency) result).getDependant();
                if(!right.contains(rightID.getTableIdentifier().replace("."+InputConfigurationSingleton.get().getFILE_ENDING(), "")) || !isCardValid(rightID)) {
                    toRemove.add(result);
                } else if(!isSplit(leftID, rightID)){
                    toRemove.add(result);
                }

            } else if(result instanceof InclusionDependency) {
                ColumnPermutation leftID = ((InclusionDependency) result).getDependant();
                if(!isDepValid(left, leftID.getColumnIdentifiers())){
                    toRemove.add(result);
                    continue;
                }
                ColumnPermutation rightID = ((InclusionDependency) result).getReferenced();
                if(!isDepValid(right, rightID.getColumnIdentifiers())) {
                    toRemove.add(result);
                } else if(!isSplit(leftID, rightID)){
                    toRemove.add(result);
                }

/*
                for (ColumnIdentifier id : rightID.getColumnIdentifiers()) {
                    if(!compareCard(leftID.getColumnIdentifiers().get(0), id, ">=", Double::valueOf, aLong -> (aLong*(0.6)))){
                        toRemove.add(result);
                    }
                }

 */

                /*
                for (ColumnIdentifier id : rightID.getColumnIdentifiers()) {
                    if(!compareCard(leftID.getColumnIdentifiers().get(0), id, "=", Double::valueOf, Double::valueOf)){
                        toRemove.add(result);
                    }
                }

                 */




            }
        }
        //System.out.println("Removing: " + toRemove);
        for (Result result : toRemove) {
            results.remove(result);
        }
    }

    private boolean isSplit(ColumnPermutation leftID, ColumnPermutation rightID) {
        return true; //@TODO
        /*
        if(!Config.split)
            return true;
        for (ColumnIdentifier id_left : leftID.getColumnIdentifiers()) {
            for (ColumnIdentifier id_right : rightID.getColumnIdentifiers()) {
                if(!isSplit(id_left, id_right))
                    return false;
            }
        }
        return true;

         */
    }

    private boolean isSplit(ColumnCombination leftID, ColumnIdentifier rightID) {
        return true; //@TODO
        /*
        if(!Config.split)
            return true;
        for (ColumnIdentifier id : leftID.getColumnIdentifiers()) {
            if(!isSplit(id, rightID))
                return false;
        }
        return true;

         */
    }

    private boolean isSplit(ColumnIdentifier leftID, ColumnIdentifier rightID) {
        return true; //@TODO
        /*
        if(!Config.split)
            return true;
        return !leftID.getTableIdentifier().equals(rightID.getTableIdentifier());

         */
    }

    private boolean isDepValid(List<String> left, Iterable<ColumnIdentifier> columnIdentifiers) {
        for (ColumnIdentifier id: columnIdentifiers) {
            if(!isCardValid(id))
                return false;
            if(!left.contains(id.getTableIdentifier().replace("."+ InputConfigurationSingleton.get().getFILE_ENDING(), "")))
                return false;
        }
        return true;
    }

    private boolean compareCard(ColumnIdentifier left, ColumnIdentifier right, String compareOperator, Function<Long, Double> functionLeft, Function<Long, Double> functionRight){
        if(InputConfiguration.cardMap.isEmpty() || !InputConfiguration.cardMap.containsKey(left.toString()) || !InputConfiguration.cardMap.containsKey(right.toString()))
            return true;
        Long originalLeft = InputConfiguration.cardMap.get(left.toString());
        Long originalRight = InputConfiguration.cardMap.get(right.toString());
        Double leftLong = functionLeft.apply(originalLeft);
        Double rightLong = functionRight.apply(originalRight);

        switch (compareOperator){
            case "=":
                return leftLong.equals(rightLong);
            case ">":
                return leftLong.compareTo(rightLong) > 0;
            case "<":
                return leftLong.compareTo(rightLong) < 0;
            case ">=":
            case "=>":
                if(leftLong.compareTo(rightLong) >= 0){
                    //System.out.println((originalRight.doubleValue()/originalLeft.doubleValue()) + " " + left + " " + right);
                    return true;
                } else {
                    //System.out.println((originalRight.doubleValue()/originalLeft.doubleValue()) + " " + left + " " + right);
                    return false;
                }
            case "<=":
            case "=<":
                return leftLong.compareTo(rightLong) <= 0;
            default:
                return false;
        }
    }
    private boolean isCardValid(ColumnIdentifier id) {
        if(InputConfiguration.cardMap.isEmpty() || !InputConfiguration.cardMap.containsKey(id.toString()))
            return true;
        if(InputConfiguration.maxCard != -1 || InputConfiguration.minCard != -1){
            Long card = InputConfiguration.cardMap.get(id.toString());
            if(InputConfiguration.maxCard != -1 && InputConfiguration.maxCard <= card)
                return false;
            else if(InputConfiguration.minCard != -1 && InputConfiguration.minCard >= card)
                return false;
        }
        return true;
    }


    private ColumnCombination convertPremutationToComb(ColumnPermutation leftID) {
        ColumnCombination columnCombination = new ColumnCombination();
        columnCombination.setColumnIdentifiers(new HashSet<>(leftID.getColumnIdentifiers()));
        return columnCombination;
    }

    private ColumnCombination convertIDtoComb(ColumnIdentifier leftID) {
        return new ColumnCombination(leftID);
    }

}
