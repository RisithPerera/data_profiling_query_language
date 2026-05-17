package de.metathesis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.metaserve.engine.Metaserve;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.listener.ComplitionListener;
import de.metaserve.util.result.ResultSet;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metathesis.structures.DatasetConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.InputStream;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseTest {

    static final String DATASET_NAME = "WDC";  // WDC, TPCH_12, TPCH_425
    static final String MODE = "HOLISTIC"; //HOLISTIC, DPAL

    Metaserve metaserve;

    @BeforeAll
    public static void initConfig() throws Exception {
        String datasetConfigs = "datasets.json";

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = BinaryTest.class.getClassLoader().getResourceAsStream(datasetConfigs);
        List<DatasetConfig> datasets = mapper.readValue(is, new TypeReference<List<DatasetConfig>>() {});

        DatasetConfig config = datasets.stream()
                .filter(d -> d.getName().equals(DATASET_NAME))
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
        executorConfiguration.setExecutorType(ExecutorConfiguration.Executor.valueOf(MODE));
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
        ProfilingContext.getInstance().clear();
    }

    void runQuery(String query){
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        for(ResultSet table : resultSetList){
            System.out.println("-- Result (showing first set of " + table.size() + ") --");
            //System.out.println(table);
            //System.out.println();
            //table.printResults();
        }
    }
}
