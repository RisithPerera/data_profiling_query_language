package de.metathesis.logic;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metaserve.util.exceptions.TablesDiscoveryException;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.Preprocessor;
import de.metathesis.ResultFormatter;
import de.metathesis.profilers.UCCProfiler;
import de.metathesis.profilers.requests.SearchSpace;
import de.metathesis.profilers.requests.UCCRequest;
import de.metathesis.profilers.results.UCCResult;
import de.metathesis.utils.Utility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("UCC Testing")
public class UCCTest {

    private static Preprocessor preprocessor;
    private static ResultFormatter resultFormatter;
    private static UCCProfiler uccProfiler;
    private static Map<String, int[]> relationIndexMap;
    private static Map<String, int[]> relationSizesMap;

    @BeforeAll
    public static void setupNode() {
        InputConfigurationSingleton.get().setDATA_SET("WDC");
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR(",");

        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        uccProfiler = new UCCProfiler(executor);

        Map<String, List<String>> relationMap = new HashMap<>();
        relationMap.put("X", getTables());

        preprocessor = Preprocessor.getInstance();
        relationIndexMap = preprocessor.initializeSearchSpace(relationMap);
        relationSizesMap = preprocessor.getAttributeSizesOf(relationIndexMap);

        resultFormatter = ResultFormatter.getInstance();
    }

    @Test
    public void testUCC() throws InputIterationException {
        int globalMaxLevel = Utility.max(relationSizesMap.values());

        for (int level = 1; level <= globalMaxLevel; level++) {
            //Filter Relations based on the column size and level
            final int[] lhsRelationIndexes = Utility.filterByLevel(relationIndexMap.get("X"), relationSizesMap.get("X"), level);

            SearchSpace lhs = new SearchSpace.Free(lhsRelationIndexes, level);
            UCCResult result = uccProfiler.profile(new UCCRequest(lhs));

            System.out.println("--------- Level: " + level + " Result Size: " + result.size());
//            for(UCCResult.UCC ucc:  result) {
//                System.out.println(resultFormatter.formatUCC(ucc));
//            }
        }
    }

    private static List<String> getTables() {
        List<String> tables = new ArrayList<>();

        String inputPath = InputConfigurationSingleton.get().getInputPath();
        File folder = new File(inputPath);

        if (!folder.exists()) {
            throw new TablesDiscoveryException(
                    TablesDiscoveryException.Reason.INPUT_FOLDER_MISSING,
                    "Input folder not found: " + inputPath,
                    inputPath
            );
        }

        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isFile() &&
                    fileEntry.getName().endsWith("." + InputConfigurationSingleton.get().getFILE_ENDING())) {
                tables.add(
                        fileEntry.getName().replace("." + InputConfigurationSingleton.get().getFILE_ENDING(), "")
                );
            }
        }


        if (tables.isEmpty()) {
            throw new TablesDiscoveryException(
                    TablesDiscoveryException.Reason.NO_TABLES_FOUND,
                    "No input files with extension '." + InputConfigurationSingleton.get().getFILE_ENDING() +
                            "' found in folder: " + new File(inputPath).getName(),
                    inputPath
            );
        }

        return tables;
    }
}
