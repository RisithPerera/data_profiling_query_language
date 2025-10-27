package de.metaserve.parser.query;

import de.metaserve.parser.graph.Condition;

import java.util.List;
import java.util.Map;

public interface Query {

    static Query get(){
        return new DPQLQuery();
    }

    void addSelection(String referenceName);

    void addCC(String referenceName, List<String> sources);

    void addCondition(String condition);

    List<String> getSelections();

    List<Condition> getConditions();

    Map<String, List<String>> getCCs();

    QueryMetadata getMetaData();

    int getNumberOfTables();

}
