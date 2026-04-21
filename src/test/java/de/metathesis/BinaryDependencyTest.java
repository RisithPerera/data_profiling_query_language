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
public class BinaryDependencyTest {
    Metaserve metaserve;

    @BeforeAll
    public static void initConfig() throws Exception {
        String datasetConfigs = "datasets.json";

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = BinaryDependencyTest.class.getClassLoader().getResourceAsStream(datasetConfigs);
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
        Preprocessor.getInstance().clearAllMaps();
    }

    @Test
    @Order(1)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(2)
    public void testB1() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(3)
    public void testB2() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y) AND UCC(X)");
    }

    @Test
    @Order(4)
    public void testB3() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(5)
    public void testB4() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(6)
    public void testB5() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(Y,Z)");
    }

    @Test
    @Order(7)
    public void testB6() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(X,Z)");
    }

    @Test
    @Order(8)
    public void testB7() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(Z,Y)");
    }

    @Test
    @Order(9)
    public void testB8() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(Z,X)");
    }

    private void runQuery(String query){
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        System.out.println(first.size()); //this is a int number I want collect on each test run and time takes to finish.
        //first.printResults();
    }
}
