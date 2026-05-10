package de.metathesis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.metaserve.engine.Metaserve;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.listener.ComplitionListener;
import de.metaserve.util.result.ResultSet;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripleDependencyTest {
    Metaserve metaserve;

    @BeforeAll
    public static void initConfig() throws Exception {
        String datasetConfigs = "datasets.json";

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = TripleDependencyTest.class.getClassLoader().getResourceAsStream(datasetConfigs);
        List<DatasetConfig> datasets = mapper.readValue(is, new TypeReference<List<DatasetConfig>>() {});

        DatasetConfig config = datasets.stream()
                .filter(d -> d.getName().equals("TPCH_12"))
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
        metaserve.addListener((ComplitionListener) (query, resultSet, totalTime, resultSize) -> {
            System.out.println("#Dependencies: " + query.getMetaData().getNumberOfReduction());
            System.out.println("#Dependency Map: " + query.getMetaData().getMap());
            System.out.println("#Candidates: " + query.getMetaData().getNumberOfCandidates());
            System.out.println("#Rows: " + resultSize);
        });
        Preprocessor.getInstance().clear();
    }

    @Test
    @Order(1)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X)");
    }

    @Test
    @Order(2)
    public void testT1() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(3)
    public void testT2() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(Z,Y) AND UCC(Y)");
    }

    @Test
    @Order(4)
    public void testT3() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(5)
    public void testT4() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND IND(Y,Z) AND UCC(Y)");
    }

    @Test
    @Order(6)
    public void testT5() {
        runQuery("SELECT X, Y, P, Q FROM CC(*) X, CC(*) Y, CC(*) P, CC(*) Q WHERE FD(X,P) AND IND(X,Y) AND FD(Y,Q)");
    }

    @Test
    @Order(7)
    public void testT6() {
        runQuery("SELECT X, Y, P, Q FROM CC(*) X, CC(*) Y, CC(*) P, CC(*) Q WHERE FD(P,X) AND IND(X,Y) AND FD(Q,Y)");
    }


    private void runQuery(String query){
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        for(ResultSet table : resultSetList){
            System.out.println("-- Result (showing first set of " + table.size() + ") --");
            //System.out.println(table);
            //System.out.println();
            //table.printResults();
        }
    }
}
