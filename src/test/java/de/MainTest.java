package de;

import de.metaserve.engine.Metaserve;
import de.metaserve.engine.QueryEngine;
import de.metaserve.model.listener.QueryExecutionListener;
import de.metaserve.model.query.Query;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.extensions.fk.ForeignKeyChecker;
import de.metaserve.util.extensions.graph.Graph;
import de.metaserve.util.extensions.mapping.MapperChecker;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class MainTest {
    @Getter
    public enum Dataset {
        ADVENTURE_WORKS("AdventureWorks"),
        MUSIC("musicBrainzMock"),
        POKEMON("PK"),
        PM("PMData"),
        Test("Test"),
        TPCDI("TPCDI"),
        TPCE("TPCE"),
        TPCH("TPCHNEW");
        private final String name;
        Dataset(String name) {
            this.name = name;
        }
    }

    @Getter
    public enum QuerySt{
        FOREIGN_KEY_1("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y)"),
        FOREIGN_KEY_2("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y)"),
        FOREIGN_KEY_3("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND SPLIT(X,Y) AND CARDINALITY(X) > 5"),
        FOREIGN_KEY_4("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND SPLIT(X,Y) AND CARDINALITY(X) > 10"),
        FOREIGN_KEY_5("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND CARDINALITY(X) > 0"),

        CHAIN_IND("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE IND(X,Y) AND IND(Y,Z)"),
        CHAIN_FD("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND FD(Y,Z)"),

        SCHEMA_MATCH_1("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND SPLIT(X,Y)"),
        SCHEMA_MATCH_2("SELECT X, Y, Z, A FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS A WHERE IND(X,Y) AND FD(Y,Z) AND FD(X,A) AND NOT UCC(X) AND NOT UCC(Y)"),

        UCC("SELECT X FROM CC(*) AS X WHERE UCC(X)"),
        FD("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y)"),
        IND("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y)"),

        IND_SPLIT("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND SPLIT(X,Y)"),

        CYCLE_IND("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND IND(Y,X) AND CARDINALITY(X) > 0"),
        CYCLE_FD("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND FD(Y,X)"),
        INDEX("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND IND(X,Y)"),
        MEANINGFUL_FD("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND NOT UCC(X)"),

        CHAIN_IND_FOUR("SELECT X, Y, Z, A FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS A WHERE IND(X,Y) AND IND(Y,Z) AND IND(Z,A) AND CARDINALITY(X) > 0"),
        CYCLE_MIX("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND IND(Y,X) AND NOT UCC(X)"),
        CYCLE_MIX_REV("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND IND(Z,Y)"),

        ;


        private final String query;
        QuerySt(String query){
            this.query = query;
        }
    }

    Metaserve metaserve;

    @BeforeEach
    public void setup(){
        boolean cache = false;
        InputConfiguration.path = new File(System.getProperty("user.dir")).getParent();
        InputConfiguration inputConfig = EngineConfigurationSingleton.get().setCache(cache).getInputConfig();
        //fdb1-mb2, mb1-dis2, mb1-fdb2, s1a-s2b, s1a-s3b, s3a-s4b
        //inputConfig.setDATA_SET(Dataset.Test.getName());
        inputConfig.setFILE_MAX_ROWS(1000);
        inputConfig.setDATA_SET("Paper");
        //inputConfig.setDATA_SET("schema3_schema3-it/target");
        inputConfig.setFILE_VALUE_SEPARATOR(";");
        inputConfig.setNARY(true);
        metaserve = new Metaserve();
        metaserve.addListener(new QueryExecutionListener() {
            @Override
            public void onEvent(QueryEngine.QueryState event, Query query) {
                System.out.println("State change:" + event);
            }

            @Override
            public void onQueryCompleted(Query query, List<ResultSet> resultSet, long totalTime, int resultSize) {
                System.out.println(query.getMetaData());
            }

            @Override
            public void onEngineClosed() {
            }

            @Override
            public void onEvent(String event) {
            }
        });
    }

    @Test
    public void main() {
        String query = QuerySt.FOREIGN_KEY_2.getQuery();
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        System.out.println(resultSetList.get(0));
    }

    public void oldPrint(List<ResultSet> resultSetList){
        for (ResultSet table : resultSetList) {
            System.out.println(table.getColumnNames());
            //System.out.println(table.size());

            for(List<String> row : table.getRows()){
                System.out.println(row);
            }

        }
    }

    public void mapperCheck(List<ResultSet> resultSetList){
        MapperChecker mapperChecker = new MapperChecker();
        mapperChecker.check(resultSetList.get(0));
        //mapperChecker.checkFD(resultSetList.get(0));
        //ForeignKeyChecker fkc = new ForeignKeyChecker();
        //fkc.check(resultSetList.get(0));
    }

    public void graphCheck(List<ResultSet> resultSetList){
        Graph graph = new Graph(resultSetList.get(0));
        ResultSet resultSet = graph.getResult();
        System.out.println(resultSet.size());
        System.out.println(resultSet);

        ForeignKeyChecker fkc = new ForeignKeyChecker();
        fkc.check(resultSet);
    }
}
