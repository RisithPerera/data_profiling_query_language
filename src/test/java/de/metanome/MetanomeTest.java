package de.metanome;

import de.metanome.algorithm_integration.results.BasicStatistic;
import de.metanome.algorithm_integration.results.Result;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


@RunWith(value = Parameterized.class)
public class MetanomeTest {
    String[] files;
    String inputFolder;
    String outputFolder;
    int[] numbers;

    public MetanomeTest(String dataSetName, String[] fileNames, int[] numbers){
        this.files = fileNames;
        this.numbers = numbers;
        InputConfigurationSingleton.get().setDATA_SET(dataSetName);
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                    "Test",
                    new String[]{"Dependant", "Referenced", "Ref"},
                    new int[]{9, 3, 24, 8, 36, 2}
                },
                {
                    "TPCH",
                    new String[]{"customer", "lineitem", "nation", "orders", "part", "region", "supplier"},
                    new int[]{9, 3, 24, 8, 36, 2}
                }
        });
    }

    @Before
    public void setUp() {
        inputFolder = InputConfigurationSingleton.get().getInputPath();
        outputFolder = InputConfigurationSingleton.get().getOutputPath();
    }

    @Test
    public void correctPaths() {
        assertTrue(inputFolder, inputFolder.contains("io" + File.separator + "data"));
        assertTrue(outputFolder, outputFolder.contains("io" + File.separator +"results"));
    }

    @Test
    public void testForExecuteUCCParallel() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeUCC(files);
        assertNotNull(results);
        assertEquals(numbers[0], results.size());

        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        List<Result> results2 = metanome.executeUCC(files);
        assertNotNull(results2);
        assertEquals(numbers[0], results2.size());
    }

    @Test
    public void testExecuteUCCSequential() {
        for (String fileName : files) {
            EngineConfigurationSingleton.get().setCache(false);
            Metanome metanome = Metanome.getInstance();
            List<Result> results = metanome.executeUCC(fileName);
            assertNotNull(results);
            assertEquals(numbers[1], results.size());

            EngineConfigurationSingleton.get().setCache(true);
            metanome = Metanome.getInstance();
            List<Result> results2 = metanome.executeUCC(fileName);
            assertNotNull(results2);
            assertEquals(numbers[1], results2.size());
        }
    }

    @Test
    public void testExecuteUCCCombined() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeUCC(files);
        assertNotNull(results);
        assertEquals(numbers[0], results.size());

        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        for (String fileName : files) {
            List<Result> results2 = metanome.executeUCC(fileName);
            assertNotNull(results2);
            assertEquals(numbers[1], results2.size());
        }
    }

    @Test
    public void testForExecuteFDParallel() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeFD(files);
        assertNotNull(results);
        assertEquals(numbers[2], results.size());

        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        List<Result> results2 = metanome.executeFD(files);
        assertNotNull(results2);
        assertEquals(numbers[2], results2.size());
    }

    @Test
    public void testExecuteFDSequential() {
        for (String fileName : files) {
            EngineConfigurationSingleton.get().setCache(false);
            Metanome metanome = Metanome.getInstance();
            List<Result> results = metanome.executeFD(fileName);
            assertNotNull(results);
            assertEquals(numbers[3], results.size());

            EngineConfigurationSingleton.get().setCache(true);
            metanome = Metanome.getInstance();
            List<Result> results2 = metanome.executeFD(fileName);
            assertNotNull(results2);
            assertEquals(numbers[3], results2.size());
        }
    }

    @Test
    public void testExecuteFDCombined() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeFD(files);
        assertNotNull(results);
        assertEquals(numbers[2], results.size());

        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        for (String fileName : files) {
            List<Result> results2 = metanome.executeFD(fileName);
            assertNotNull(results2);
            assertEquals(numbers[3], results2.size());
        }
    }

    @Test
    public void testForExecuteINDParallel() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeIND(files);
        assertNotNull(results);
        assertEquals(numbers[4], results.size());

        //@TODO FIX ME Sometimes there are the same Results found???
        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        List<Result> results2 = metanome.executeIND(files);
        assertNotNull(results2);
        HashSet<Result> results2Set = new HashSet<>(results2);
        System.out.println(results2);
        assertEquals(numbers[4], results2.size());
    }

    @Test
    public void testExecuteINDSequential() {
        for (String fileName : files) {
            EngineConfigurationSingleton.get().setCache(false);
            Metanome metanome = Metanome.getInstance();
            List<Result> results = metanome.executeIND(fileName);
            assertNotNull(results);
            assertEquals(numbers[5], results.size());

            EngineConfigurationSingleton.get().setCache(true);
            metanome = Metanome.getInstance();
            List<Result> results2 = metanome.executeIND(fileName);
            assertNotNull(results2);
            assertEquals(numbers[5], results2.size());
        }
    }

    //@TODO FIX ME
    @Test
    public void testExecuteINDCombined() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeIND(files);
        assertNotNull(results);
        assertEquals(numbers[4], results.size());

        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        for (String fileName : files) {
            List<Result> results2 = metanome.executeIND(fileName);
            assertNotNull(results2);
            assertEquals(numbers[5], results2.size());
        }
    }

    @Test
    public void testForExecuteCARDParallel() {
        List<Integer> expectedResults = Arrays.asList(4, 4, 3, 3, 6, 6, 5, 4, 4, 4, 3, 3);
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeCARD(files);
        assertNotNull(results);
        assertEquals(12, results.size());
        assertListEqualsCARD(expectedResults, results);
        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        List<Result> results2 = metanome.executeCARD(files);
        assertNotNull(results2);
        assertEquals(12, results2.size());
        assertListEqualsCARD(expectedResults, results);
    }

    @Test
    public void testExecuteCARDSequential() {
        List<List<Integer>> expectedResults = Arrays.asList(
                Arrays.asList(4, 4, 3, 3),
                Arrays.asList(6, 6, 5, 4),
                Arrays.asList(4, 4, 3, 3)
        );
        int i = 0;
        for (String fileName : files) {
            EngineConfigurationSingleton.get().setCache(false);
            Metanome metanome = Metanome.getInstance();
            List<Result> results = metanome.executeCARD(fileName);
            assertNotNull(results);
            assertEquals(4, results.size());
            assertListEqualsCARD(expectedResults.get(i), results);

            EngineConfigurationSingleton.get().setCache(true);
            metanome = Metanome.getInstance();
            List<Result> results2 = metanome.executeCARD(fileName);
            assertNotNull(results2);
            assertEquals(4, results2.size());
            assertListEqualsCARD(expectedResults.get(i), results2);
            i++;
        }
    }

    @Test
    public void testExecuteCARDCombined() {
        EngineConfigurationSingleton.get().setCache(false);
        Metanome metanome = Metanome.getInstance();
        List<Result> results = metanome.executeCARD(files);
        assertNotNull(results);
        assertEquals(12, results.size());

        EngineConfigurationSingleton.get().setCache(true);
        metanome = Metanome.getInstance();
        for (String fileName : files) {
            List<Result> results2 = metanome.executeCARD(fileName);
            assertNotNull(results2);
            assertEquals(4, results2.size());
        }
    }

    public void assertListEqualsCARD(List<Integer> expected, List<Result> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Long value = Long.valueOf(expected.get(i));
            BasicStatistic resultStatistic = (BasicStatistic) actual.get(i);
            Long actualValue = (Long) resultStatistic.getStatisticMap().values().iterator().next().getValue();
            assertEquals(value, actualValue);
        }
    }

}
