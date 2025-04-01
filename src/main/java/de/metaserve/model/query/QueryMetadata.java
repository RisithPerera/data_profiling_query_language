package de.metaserve.model.query;

import de.metaserve.engine.QueryEngine;
import jnr.ffi.annotations.In;

import java.util.HashMap;
import java.util.Map;

public class QueryMetadata {
    HashMap<String, Integer> numberOfReduction = new HashMap<>();
    HashMap<String, Integer> numberOfEdges = new HashMap<>();
    long currentTime = -1L;
    long startTime = -1L;
    long overallTime = -1L;
    long parseTime = -1L;
    long optimizeTime = -1L;
    long executorTime = -1L;
    long executorGraphTime = -1L;
    long dependencyTime = -1L;
    long resultRetrial = -1L;

    public void update(QueryEngine.QueryState state){
        long timeNow = System.currentTimeMillis();
        long timeSinceLastUpdate = timeNow - currentTime;
        switch (state){
            case QUERY_PARSING:
                parseTime = timeSinceLastUpdate;
                break;
            case QUERY_OPTIMIZING:
                optimizeTime = timeSinceLastUpdate;
                break;
            case QUERY_WAITING_FOR_METANOME:
                dependencyTime = timeSinceLastUpdate;
                break;
            case COMPUTED_MIN:
                executorGraphTime = timeSinceLastUpdate;
                break;
            case QUERY_EXECUTING:
                executorTime = resultRetrial + executorGraphTime + dependencyTime;
                overallTime = timeNow - startTime;
                break;
            case QUERY_RESULT:
                resultRetrial = timeSinceLastUpdate;
                break;
        }
        currentTime = timeNow;
    }

    public Map<String, Integer> getMap(){
        return numberOfReduction;
    }
    public Integer getNumberOfReduction(){
        return numberOfReduction.values().stream().mapToInt(x -> x).sum();
    }

    public Integer getNumberOfCandidates(){
        Integer result = 1;
        for (String name : numberOfReduction.keySet()){
            result = result * numberOfReduction.get(name) * numberOfEdges.get(name);
        }
        return result;
    }

    public void addStat(String name, Integer number){
        numberOfEdges.put(name, numberOfEdges.getOrDefault(name, 0) + 1);
        if(numberOfReduction.containsKey(name))
            number += numberOfReduction.get(name);
        numberOfReduction.put(name, number);
    }

    public long getTime(){
        return overallTime;
    }

    public void setTime(long time) {
        this.startTime = time;
        this.currentTime = time;
    }

    @Override
    public String toString() {
        return "QueryMetadata{" +
                "#reduction=" + getNumberOfReduction() +
                ", overallTime=" + overallTime +
                ", parseTime=" + parseTime +
                ", optimizeTime=" + optimizeTime +
                ", executorTime=" + executorTime +
                ", resultRetrial=" + resultRetrial +
                ", executorGraphTime=" + executorGraphTime +
                ", dependencyTime=" + dependencyTime +
                ", map=" + numberOfReduction +
                '}';
    }
}
