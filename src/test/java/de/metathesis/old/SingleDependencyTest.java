package de.metathesis.old;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.metaserve.engine.Metaserve;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.listener.CompletionListener;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metathesis.structures.DatasetConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

public class SingleDependencyTest {

    static List<DatasetConfig> datasets;

    record ResultRow(String dataset, long dpalCount, long dpalTime, long hollCount, long hollTime) {}

    @BeforeAll
    public static void initConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SingleDependencyTest.class.getClassLoader().getResourceAsStream("datasets_test.json");
        datasets = mapper.readValue(is, new TypeReference<List<DatasetConfig>>() {});
    }

    @Test
    public void testUCC() {
        printTable("UCC", runForAllDatasets("SELECT X FROM CC(*) X WHERE UCC(X)"));
    }

//    @Test
//    public void testFD() {
//        printTable("FD", runForAllDatasets("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y)"));
//    }

//    @Test
//    public void testIND() {
//        printTable("IND", runForAllDatasets("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y)"));
//    }

    private List<ResultRow> runForAllDatasets(String query) {
        return datasets.stream()
                .map(config -> {
                    long[] dpal = runSingle(config, "DPAL", query);
                    long[] holl = runSingle(config, "HOLISTIC", query);
                    return new ResultRow(config.getName(), dpal[0], dpal[1], holl[0], holl[1]);
                })
                .toList();
    }

    private long[] runSingle(DatasetConfig config, String mode, String query) {
        System.out.printf("Running: %s on Dataset: %s\n", mode, config.getName());
        InputConfiguration inputConfig = EngineConfigurationSingleton.get().getInputConfig();
        inputConfig.setDATA_SET(config.getName());
        inputConfig.setFILE_VALUE_SEPARATOR(config.getSeparator());
        inputConfig.setFILE_QUOTE_CHAR(config.getQuoteChar());
        inputConfig.setFILE_ENDING(config.getFileEnding());
        inputConfig.setFILE_HAS_HEADER(config.isHasHeader());

        ExecutorConfiguration executorConfig = EngineConfigurationSingleton.get().getExecutorConfig();
        executorConfig.setExecutorType(ExecutorConfiguration.Executor.valueOf(mode));

        long[] result = {-1, -1};

        Metaserve metaserve = new Metaserve();
        metaserve.addListener((CompletionListener) (q, resultSet, totalTime, resultSize) -> {
            result[0] = resultSize;
        });

        try {
            long time = System.currentTimeMillis();
            metaserve.executeQuery(query);
            result[1] = System.currentTimeMillis() - time;
        } catch (Exception e) {
            System.err.println("Failed: " + config.getName() + " / " + mode + " -> " + e.getMessage());
        }finally {
            metaserve.close();
        }

        return result;
    }

    private void printTable(String queryType, List<ResultRow> rows) {
        String header = String.format("%-20s | %12s | %12s | %12s | %12s", "Dataset", "DPAL count", "DPAL time", "HOLL count", "HOLL time");
        String separator = "-".repeat(header.length());

        System.out.println("\n=== " + queryType + " ===");
        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);

        for (ResultRow row : rows) {
            System.out.printf("%-20s | %12d | %12d | %12d | %12d%n", row.dataset(), row.dpalCount(), row.dpalTime(), row.hollCount(), row.hollTime());
        }

        System.out.println(separator);
    }
}