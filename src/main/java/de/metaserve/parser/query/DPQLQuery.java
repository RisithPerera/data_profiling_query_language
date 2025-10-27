package de.metaserve.parser.query;

import de.metaserve.parser.graph.Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DPQLQuery implements Query {
    List<String> selections = new ArrayList<>();
    List<Condition> conditions = new ArrayList<>();
    Map<String, List<String>> ccFunctions = new HashMap<>();
    QueryMetadata metadata = new QueryMetadata();

    public DPQLQuery(){}

    @Override
    public void addSelection(String refName) {
        selections.add(refName);
    }

    @Override
    public void addCC(String refName, List<String> sources) {
        ccFunctions.put(refName, sources);
    }

    @Override
    public void addCondition(String condition) {
        conditions.add(Condition.build(condition, ccFunctions));
    }

    @Override
    public List<String> getSelections() {
        return selections;
    }

    @Override
    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public Map<String, List<String>> getCCs() {
        return ccFunctions;
    }

    @Override
    public QueryMetadata getMetaData() {
        return metadata;
    }

}
