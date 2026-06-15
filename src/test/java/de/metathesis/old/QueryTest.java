package de.metathesis.old;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.metaserve.engine.Metaserve;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.listener.CompletionListener;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metathesis.structures.DatasetConfig;
import de.metathesis.structures.ResultTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

public class QueryTest {
    Metaserve metaserve;

    @BeforeAll
    public static void initConfig() throws Exception {
        String datasetConfigs = "datasets.json";

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = QueryTest.class.getClassLoader().getResourceAsStream(datasetConfigs);
        List<DatasetConfig> datasets = mapper.readValue(is, new TypeReference<List<DatasetConfig>>() {});

        DatasetConfig config = datasets.stream()
                .filter(d -> d.getName().equals("WDC"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Dataset not found!"));

        InputConfiguration inputConfig = EngineConfigurationSingleton.get().getInputConfig();
        inputConfig.setDATA_SET(config.getName());
        inputConfig.setFILE_VALUE_SEPARATOR(config.getSeparator());
        inputConfig.setFILE_QUOTE_CHAR(config.getQuoteChar());
        inputConfig.setFILE_ENDING(config.getFileEnding());
        inputConfig.setFILE_HAS_HEADER(config.isHasHeader());
        inputConfig.setNORMALIZE_RESULTS(false);

        ExecutorConfiguration executorConfiguration = EngineConfigurationSingleton.get().getExecutorConfig();
        executorConfiguration.setExecutorType(ExecutorConfiguration.Executor.valueOf("HOLISTIC")); //HOLISTIC, DPAL
    }

    @BeforeEach
    public void setup() {
        metaserve = new Metaserve();
        metaserve.addListener((CompletionListener) (query, resultSet, totalTime, resultSize) -> {
            System.out.println("#Dependencies: " + query.getMetaData().getNumberOfReduction());
            System.out.println("#Dependency Map: " + query.getMetaData().getMap());
            System.out.println("#Candidates: " + query.getMetaData().getNumberOfCandidates());
            System.out.println("#Rows: " + resultSize);
        });
    }

    @Test
    public void testUCC() {
        runQuery("SELECT X FROM CC(*) X WHERE UCC(X)");
    }

    @Test
    public void testIND() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y)");
    }

    @Test
    public void testFD() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y)");
    }

    @Test
    public void testQ0() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Y) AND IND(Y,Z)");
    }

    @Test
    public void testQ1() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(Y)");
    }

    @Test
    public void testQ2() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    public void testQ3() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND FD(Y,W)");
    }

    @Test
    public void testQ4() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    public void testQ5() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND IND(Y,Z) AND UCC(Y)");
    }

    @Test
    public void testQ6() {
        runQuery("SELECT P, Q, X, Y FROM CC(*) P, CC(*) Q, CC(*) X, CC(*) Y WHERE FD(X,P) AND IND(X,Y) AND FD(Y,Q)");
    }

    @Test
    public void testQ7() {
        runQuery("SELECT P, Q, X, Y FROM CC(*) P, CC(*) Q, CC(*) X, CC(*) Y WHERE FD(P,X) AND IND(X,Y) AND FD(Q,Y)");
    }

    @Test
    public void testQ8() {
        runQuery("SELECT X, Y, Z, P, Q, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) Q, CC(*) R " +
                "WHERE FD(X,P) AND FD(X,Q) AND IND(X,Y) AND IND(Y,Z) AND FD(Y,R) AND UCC(R)");
    }

    @Test
    public void testQ9() {
        runQuery("SELECT X, Y, Z, P, Q, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) Q, CC(*) R " +
                "WHERE FD(X,Y) AND FD(Y,Z) AND IND(Z,R) AND IND(X,P) AND IND(X,Q) AND UCC(Q)");
    }

    @Test
    public void testQ10() {
        runQuery("SELECT X, Y, Z, P, Q, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) Q, CC(*) R " +
                "WHERE IND(X,Z) AND FD(Z,P) AND FD(Z,Q) AND IND(X,Y) AND IND(Y,R) AND UCC(R)");
    }

    @Test
    public void testExtra() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE UCC(Z) AND FD(Z,X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    public void testQ11() {
        runQuery("SELECT X, Y, Z, P, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) R " +
                "WHERE IND(X,Z) AND FD(Z,P) AND IND(X,Y) AND IND(Y,R) AND UCC(R)");
    }

    private void runQuery(String query){
        List<ResultTable> resultSetList = metaserve.executeQuery(query);
        for(ResultTable table : resultSetList){
            System.out.println("-- Result (showing first set of " + table.size() + ") --");
            //System.out.println(table);
            //System.out.println();
            //table.printResults();
        }
    }
}
