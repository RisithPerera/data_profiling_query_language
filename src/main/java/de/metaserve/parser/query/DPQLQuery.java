package de.metaserve.parser.query;

import de.metaserve.parser.graph.Condition;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.io.File;
import java.util.*;

public class DPQLQuery implements Query {
    List<String> selections = new ArrayList<>();
    List<Condition> conditions = new ArrayList<>();
    Map<String, List<String>> ccFunctions = new HashMap<>();
    QueryMetadata metadata = new QueryMetadata();

    int maxTables = 0;

    public DPQLQuery(){
        String inputPath = InputConfigurationSingleton.get().getInputPath();
        File folder = new File(inputPath);
        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isFile() &&
                    fileEntry.getName().endsWith("." + InputConfigurationSingleton.get().getFILE_ENDING())) {
                maxTables++;
            }
        }
    }

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

    @Override
    public int getNumberOfTables() {
        return maxTables;
    }

}
