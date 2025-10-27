package de;

import de.metaserve.engine.Metaserve;
import de.metaserve.engine.QueryEngine;
import de.metaserve.util.listener.ComplitionListener;
import de.metaserve.util.listener.QueryExecutionListener;
import de.metaserve.parser.query.Query;
import de.metaserve.util.result.ResultSet;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.extensions.fk.ForeignKeyChecker;
import de.metaserve.util.extensions.graph.Graph;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainTest {

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

        public static String name(String value){
            Dataset[] values = Dataset.values();
            String enumValue = null;
            for(Dataset eachValue : values) {
                enumValue = eachValue.getName();

                if (enumValue.equalsIgnoreCase(value)) {
                    return eachValue.name();
                }
            }
            return enumValue;
        }

        public String getName() {
            return name;
        }
    }

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

        public String getQuery() {
            return query;
        }
    }


    public enum BTWQueries{
        //33,2538
        FOREIGN_KEY("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y)"),// AND NOT UCC(X) AND SPLIT(X,Y)
        EMBEDDED_LINK("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Y) AND NOT UCC(X)"),
        EMBEDDED_EMBEDDED_LINK("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE FD(X,Z) AND IND(X,Y) AND FD(Y,W) AND NOT UCC(Y)"),
        REDUNDANT_NORMALIZATION("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE UCC(X) AND IND(X,Y) AND UCC(Y)"),
        TWO_NF_VIOLATION("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND UCC(Z) AND CONTAINS(Z,X)"),
        BCNF_VIOLATION("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND NOT UCC(X)"),
        //FOUR_NF_VIOLATION("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE MVD(X,Y) AND NOT UCC(X)"),
        IDNF_VIOLATION("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND IND(Y,X) AND SPLIT(X,Y)"),
        IDNT_VIOLATION_TWO("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND FD(X,Y)"),
        FOREIGN_KEY_RULE("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y)"),
        DUAL_JOIN_RULE("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE IND(X,Y) AND IND(Y,Z) AND UCC(Y)"),
        //THETA_JOIN_RULE("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE OD(X,Y) AND UCC(X)"),
        JOIN_SIMPLIFICATION_RULE("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE IND(X,Y) AND FD(Z,W) AND CONTAINS(Y,Z) AND NOT UCC(Z)"),
        CARDINALITY_PROPAGATION_RULE("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND FD(Y,X)"),
        UNIQUENESS_TRACKING_RULE("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE UCC(X) AND IND(X,Y) AND UCC(Y)"),
        //JOIN_TO_PREDICATE_REWRITE_RULE("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND OD(Y,Z) AND UCC(Y)"),
        REDUNDANT_ML_FEATURE("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND FD(Z,X) AND NOT UCC(Z) AND NOT UCC(X)"),
        CROSS_TABLE_REDUNDANT_ML_FEATURE("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE FD(X,Y) AND IND(X,W) AND FD(Z,W) AND NOT UCC(Z) AND NOT UCC(X)"),
        COMMON_CAUSE("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND FD(X,Z) AND NOT UCC(X)"),
        COMMON_EFFECT("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND FD(Z,Y) AND NOT UCC(Z) AND NOT UCC(X)"),
        CROSS_TABLE_COMMON_CAUSE("SELECT X, Y, A, B FROM CC(*) AS X, CC(*) AS Y, CC(*) AS A, CC(*) AS B WHERE FD(X,A) AND IND(X,Y) AND FD(Y,B) AND NOT UCC(X) AND NOT UCC(Y)"),
        CROSS_TABLE_COMMON_EFFECT("SELECT X, Y, A, B FROM CC(*) AS X, CC(*) AS Y, CC(*) AS A, CC(*) AS B WHERE FD(A,X) AND IND(X,Y) AND FD(B,Y) AND NOT UCC(B) AND NOT UCC(A)"),
        UNIQUENESS_MATCH("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(X) AND UCC(Y) AND SPLIT(X,Y)"),
        DEPENDENT_MATCH("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE IND(X,Y) AND FD(X,Z) AND FD(Y,W) AND SPLIT(X,Y) AND NOT UCC(X) AND NOT UCC(Y)"),
        DETERMINANT_MATCH("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE IND(X,Y) AND FD(Z,X) AND FD(W,Y) AND SPLIT(X,Y) AND NOT UCC(Z) AND NOT UCC(W)"),
        INCLUDED_MATCH("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE IND(X,Y) AND IND(Z,X) AND IND(W,Y) AND SPLIT(X,Y) AND SPLIT(Z,W) AND SPLIT(X,W) AND SPLIT(Y,Z)"),
        INCLUDING_MATCH("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE IND(X,Y) AND IND(X,Z) AND IND(Y,W) AND SPLIT(X,Y) AND SPLIT(Z,W) AND SPLIT(X,W) AND SPLIT(Y,Z)"),
        THREE_CLIQUE_DISCOVERY("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE IND(X,Y) AND IND(Y,Z) AND IND(Z,X) AND SPLIT(X,Y) AND SPLIT(Y,Z) AND SPLIT(Z,X)"),
        TWO_CLIQUE_DISCOVERY("SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND IND(Y,X) AND SPLIT(X,Y)"),
        HUB_DISCOVERY("SELECT X, Y, Z, W FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS W WHERE IND(X,Y) AND IND(Z,W) AND COALESCE(X,Z) AND SPLIT(X,Y) AND SPLIT(Z,W) AND SPLIT(W,Y)"),
        AUTHORITY_DISCOVERY("SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE IND(X,Y) AND IND(Z,Y) AND SPLIT(X,Z)"),
        ;


        private final String query;
        BTWQueries(String query){
            this.query = query;
        }

        public static String name(String value){
            BTWQueries[] values = BTWQueries.values();
            String enumValue = null;
            for(BTWQueries eachValue : values) {
                enumValue = eachValue.getQuery();

                if (enumValue.equalsIgnoreCase(value)) {
                    return eachValue.name();
                }
            }
            return null;
        }

        public String getQuery() {
            return query;
        }
    }
    Metaserve metaserve;
    boolean cache = false;
    String dataset = "TPCHNEW";
    @BeforeEach
    public void setup(){
        InputConfiguration inputConfig = EngineConfigurationSingleton.get().setCache(cache).getInputConfig();
        //fdb1-mb2, mb1-dis2, mb1-fdb2, s1a-s2b, s1a-s3b, s3a-s4b
        //inputConfig.setDATA_SET(Dataset.MUSIC.getName());
        //inputConfig.setFILE_MAX_ROWS(1000);
        inputConfig.setFILE_VALUE_SEPARATOR(",");
        inputConfig.setFILE_QUOTE_CHAR("\'");
        inputConfig.setFILE_ENDING("csv");
        inputConfig.setFILE_HAS_HEADER(true);
        //inputConfig.setNARY(false);
        inputConfig.setVALIDATE_PARALLEL(true);
        //inputConfig.setMAX_SEARCH_SPACE_LEVEL(1);
        //inputConfig.setFILE_CHAR_SET(StandardCharsets.US_ASCII);
        //inputConfig.setFILE_QUOTE_CHAR("'");
        inputConfig.setDATA_SET(dataset);
        //inputConfig.setNARY(true);
        metaserve = new Metaserve();
        metaserve.addListener((ComplitionListener) (query, resultSet, totalTime, resultSize) -> {
            System.out.println("#Dependencies: " + query.getMetaData().getNumberOfReduction());
            System.out.println("#Dependency Map" + query.getMetaData().getMap());
            System.out.println("#Candidates: " + query.getMetaData().getNumberOfCandidates());
            System.out.println("#Rows: " + resultSize);
        });
    }

    @Test
    public void main() {
        //String query = BTWQueries.FOREIGN_KEY.getQuery();
        String query = "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND UCC(X)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        //System.out.println(resultSetList.get(0).getColumnNames());
        System.out.println(resultSetList.get(0).getRows2());
        //fkCheck(resultSetList);
    }

    @Test
    public void Spind() {
        String query = QuerySt.IND.getQuery();
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    @Test
    public void BTWTest(){
        List<String> queries = Arrays.stream(BTWQueries.values()).map(BTWQueries::getQuery).collect(Collectors.toList());
        List<String> datasets = Arrays.asList(Dataset.TPCH.getName(), Dataset.ADVENTURE_WORKS.getName());

        for (String query : queries) {
            System.out.println("Query(" + ANSI_GREEN + BTWQueries.name(query) + ANSI_RESET + "): " + query);
            for (String localDataset : datasets){
                System.out.println("Dataset(" + ANSI_GREEN + Dataset.name(localDataset) + ANSI_RESET + "): " + localDataset);
                this.dataset = localDataset;
                setup();
                metaserve.executeQuery(query);
            }
        }
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

    public void fkCheck(List<ResultSet> resultSetList){
        ForeignKeyChecker fkc = new ForeignKeyChecker();
        fkc.check(resultSetList.get(0));
    }

    public void mapperCheck(List<ResultSet> resultSetList){
       // MapperChecker mapperChecker = new MapperChecker();
        //mapperChecker.check(resultSetList.get(0));
        //mapperChecker.checkFD(resultSetList.get(0));
    }

    public void graphCheck(List<ResultSet> resultSetList){
        Graph graph = new Graph(resultSetList.get(0));
        ResultSet resultSet = graph.getResult();
        System.out.println(resultSet.size());
        System.out.println(resultSet);

        fkCheck(resultSetList);
    }
}
