package de.metaserve.e2e;

import de.metaserve.engine.Metaserve;
import de.metaserve.parser.ANTLRParser;
import de.metaserve.parser.Parser;
import de.metaserve.util.configuration.ExecutorConfiguration;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import de.metaserve.util.singletons.ExecutorConfigurationSingelton;

import de.metaserve.util.singletons.InputConfigurationSingleton;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;


/**
 * @TODO Implement real tests
 */
public class ParserTest {

    @Before
    public void setup(){
        EngineConfigurationSingleton.get().setLog(false).setCache(false);
    }

    @Test
    public void notOperation() {
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND SPLIT(X,Y) AND CARDINALITY(X) > 2";
        parse(sql);
    }

    @Test
    public void completeStatementMusicBrainz() {
        //Config.rootFolder = "D://deployment-1.2-SNAPSHOT-package_with_tomcat_pc/backend/WEB-INF/classes/inputData/mbdump";
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(1);
        InputConfigurationSingleton.get().setNARY(false);
        InputConfigurationSingleton.get().setFILE_MAX_ROWS(100);
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y) " +
                "AND UCC(X) " +
                "AND SPLIT(X,Y)";
        parse(sql);
        //4312 Records
    }

    @Test
    public void computeMusicBrainzCard() {
        //Config.rootFolder = "D://deployment-1.2-SNAPSHOT-package_with_tomcat_pc/backend/WEB-INF/classes/inputData/mbdump";
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setFILE_MAX_ROWS(10000);
        String sql = "SELECT " +
                "X " +
                "FROM CC(*) AS X " +
                "WHERE " +
                "CARD(X) > 2";
        parse(sql);
        //empty table
        //only to compute CARDS for cache
    }

    /*
    @Test
    public void readMinMaxTable(){

//        Column 1: min=3249924.0, max=4336515.0
//Column 2: min=1.0, max=200000.0
//Column 3: min=1.0, max=10000.0
//Column 4: min=1.0, max=7.0


        Config.rootFolder = "D:/BTW/metaserve/metaserve/data/TPCH/";
                String fileName = Config.rootFolder;
                List<Double> data1 = readCSV(fileName+"supplier.csv", 0);
                List<Double> data2 = readCSV(fileName+"lineitem.csv", 2);
                diff(data1, data2);
                //List<List<Double>> minMax = getMinMax(data);
                //System.out.println("Minimum and Maximum Values for Each Column:");
                //for (int i = 0; i < minMax.size(); i++) {
                 //   System.out.println("Column " + (i+1) + ": min=" + minMax.get(i).get(0) + ", max=" + minMax.get(i).get(1));
                //}
    }

    public static void diff(List<Double> a1, List<Double> a2){
        Set<Double> mySet1 = new HashSet<>(a1);
        List<Double> myNewList1 = new ArrayList<>(mySet1);
        Set<Double> mySet2 = new HashSet<>(a2);
        List<Double> myNewList2 = new ArrayList<>(mySet2);
        myNewList1.sort(Double::compareTo);
        myNewList2.sort(Double::compareTo);
        System.out.println(myNewList2.size() + " " + myNewList1.size());
        for (int i = 0; i < myNewList1.size(); i++) {
            if(!myNewList1.get(i).equals(myNewList2.get(i)))
                System.out.println(myNewList1.get(i) + " " + myNewList2.get(i));
        }
    }
    
    public static List<Double> readCSV(String fileName, int col) {
        List<Double> data = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = "";
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                String parse = values[col].replace("\"", "");
                data.add(Double.parseDouble(parse));
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static List<List<Double>> getMDiff(List<List<Double>> data) {
        int numCols = data.get(0).size();
        List<List<Double>> minMax = new ArrayList<>();
        for (int i = 0; i < numCols; i++) {
            Double[] col = new Double[data.size()];
            for (int j = 0; j < data.size(); j++) {
                col[j] = data.get(j).get(i);
            }
            Arrays.sort(col);
            List<Double> minMaxValues = new ArrayList<>();
            minMaxValues.add(col[0]);  // minimum value
            minMaxValues.add(col[col.length-1]);  // maximum value
            minMax.add(minMaxValues);
        }
        return minMax;
    }

    public static List<List<Double>> getMinMax(List<List<Double>> data) {
        int numCols = data.get(0).size();
        List<List<Double>> minMax = new ArrayList<>();
        for (int i = 0; i < numCols; i++) {
            Double[] col = new Double[data.size()];
            for (int j = 0; j < data.size(); j++) {
                col[j] = data.get(j).get(i);
            }
            Arrays.sort(col);
            List<Double> minMaxValues = new ArrayList<>();
            minMaxValues.add(col[0]);  // minimum value
            minMaxValues.add(col[col.length-1]);  // maximum value
            minMax.add(minMaxValues);
        }
        return minMax;
    }
    */

    @Test
    public void unkownBug() {
        InputConfigurationSingleton.get().setDATA_SET("TPCH");
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT X,Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND SPLIT(X,Y) AND CARDINALITY(X) > 7";
        parse(sql);
        //1315 Records

    }

    /*
    public void createMusicBrainzMock(){
        File folder = new File(getResultFolderPath());
        File outFolder = new File(getFolderPath());
        System.out.println(outFolder.getAbsolutePath());
        for (File fileEntry : folder.listFiles()) {
            if(!fileEntry.isFile()){
                String name = fileEntry.getName() + ".csv";
                try {
                    Files.createFile(Path.of(outFolder.getAbsolutePath(), name));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public String getResultFolderPath(){
        String basePath = new File("").getAbsolutePath();
        int index = basePath.lastIndexOf("\\");
        String path = basePath.substring(0, index) + "/io/measurements/Test/";
        return path;
    }

    public String getFolderPath(){
        return new File("").getAbsolutePath() + "/data/musicBrainzMock";
    }
     */

    @Test
    public void completeStatementMusicBrainzCard() {
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(1);
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y) " +
                "AND UCC(X) " +
                "AND SPLIT(X,Y) " +
                "AND CARD(X) > 2";
        parse(sql);
        //1315 Records
    }

    @Test
    public void completeStatementMusicBrainzSplit() {
        //Config.rootFolder = "D://deployment-1.2-SNAPSHOT-package_with_tomcat_pc/backend/WEB-INF/classes/inputData/mbdump";
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(1);
        ExecutorConfigurationSingelton.get().setOutputType(ExecutorConfiguration.Output.FILE);
        //ParserConfigurationSingleton.get().fileName = "split.csv";
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y) " +
                "AND UCC(X) " +
                "AND SPLIT(X,Y)";
        parse(sql);
        //1315 Records
    }

    @Test
    public void completeStatementMusicBrainzCard10() {
        //Config.rootFolder = "D://deployment-1.2-SNAPSHOT-package_with_tomcat_pc/backend/WEB-INF/classes/inputData/mbdump";
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(1);
        ExecutorConfigurationSingelton.get().setOutputType(ExecutorConfiguration.Output.FILE);
        //ParserConfigurationSingleton.get().fileName = "card2.csv";
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y) " +
                "AND UCC(X) " +
                "AND SPLIT(X,Y) " +
                "AND CARD(X) > 2";
        parse(sql);
        //1315 Records
    }

    @Test
    public void readAllMusicBrainzINDs() {
        //Config.rootFolder = "D://deployment-1.2-SNAPSHOT-package_with_tomcat_pc/backend/WEB-INF/classes/inputData/mbdump";
        EngineConfigurationSingleton.get().setCache(true);
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(1);
        InputConfigurationSingleton.get().setNARY(false);
        InputConfigurationSingleton.get().setFILE_MAX_ROWS(100);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y)";
        parse(sql);
        //262970 Records
    }


    @Test
    public void completeStatementMusicBrainzUCC() {
        //Config.rootFolder = "D://deployment-1.2-SNAPSHOT-package_with_tomcat_pc/backend/WEB-INF/classes/inputData/mbdump";
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR("\t");
        InputConfigurationSingleton.get().setFILE_HAS_HEADER(false);
        InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(1);
        InputConfigurationSingleton.get().setNARY(false);
        InputConfigurationSingleton.get().setFILE_MAX_ROWS(1000);
        String sql = "SELECT " +
                "X " +
                "FROM CC(*) AS X " +
                "WHERE " +
                "UCC(X)";
        parse(sql);
        //#608
    }

    @Test
    public void completeStatement() {
        String sql = "SELECT X, Y, Z FROM CC(Dependant) AS X, CC(Referenced) AS Y, CC(Dependant) AS Z WHERE IND(X,Y) AND UCC(X) AND UCC(Z) AND SPLIT(X,Y) AND SIZE(X) <= 2";
        parse(sql);
        /**
         * ┌──────────────────────────┬─────────────────────────┬─────────────────────────┐
         * │            X             │            Y            │            Z            │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │[DEPENDANT.csv."Key_UCC_1"│[REFERENCED.csv."Key_UCC_│[DEPENDANT.csv."Key_UCC_1│
         * │            ]             │           1"]           │           "]            │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_UCC_1"│[REFERENCED.csv."Key_NON_│[DEPENDANT.csv."Key_UCC_2│
         * │            ]             │         UCC_1"]         │           "]            │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_UCC_2"│[REFERENCED.csv."Key_UCC_│[DEPENDANT.csv."Key_NON_U│
         * │            ]             │           2"]           │         CC_1",          │
         * │                          │                         │DEPENDANT.csv."Key_NON_UC│
         * │                          │                         │          C_2"]          │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_UCC_2"│[REFERENCED.csv."Key_NON_│                         │
         * │            ]             │         UCC_2"]         │                         │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_NON_UC│[REFERENCED.csv."Key_NON_│                         │
         * │          C_1",           │         UCC_1",         │                         │
         * │DEPENDANT.csv."Key_NON_UCC│REFERENCED.csv."Key_NON_U│                         │
         * │           _2"]           │         CC_2"]          │                         │
         * └──────────────────────────┴─────────────────────────┴─────────────────────────┘
         */
    }

    @Test
    public void completeStatementCache() {
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y, Z " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y, CC(Dependant) AS Z " +
                "WHERE " +
                "IND(X,Y) " +
                "AND UCC(X) " +
                "AND UCC(Z) " +
                "AND SPLIT(X,Y) " +
                "AND SIZE(X) <= 2";
        parse(sql);
        /**
         * ┌──────────────────────────┬─────────────────────────┬─────────────────────────┐
         * │            X             │            Y            │            Z            │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │[DEPENDANT.csv."Key_UCC_1"│[REFERENCED.csv."Key_UCC_│[DEPENDANT.csv."Key_UCC_1│
         * │            ]             │           1"]           │           "]            │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_UCC_1"│[REFERENCED.csv."Key_NON_│[DEPENDANT.csv."Key_UCC_2│
         * │            ]             │         UCC_1"]         │           "]            │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_UCC_2"│[REFERENCED.csv."Key_UCC_│[DEPENDANT.csv."Key_NON_U│
         * │            ]             │           2"]           │         CC_1",          │
         * │                          │                         │DEPENDANT.csv."Key_NON_UC│
         * │                          │                         │          C_2"]          │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_UCC_2"│[REFERENCED.csv."Key_NON_│                         │
         * │            ]             │         UCC_2"]         │                         │
         * ├──────────────────────────┼─────────────────────────┼─────────────────────────┤
         * │[DEPENDANT.csv."Key_NON_UC│[REFERENCED.csv."Key_NON_│                         │
         * │          C_1",           │         UCC_1",         │                         │
         * │DEPENDANT.csv."Key_NON_UCC│REFERENCED.csv."Key_NON_U│                         │
         * │           _2"]           │         CC_2"]          │                         │
         * └──────────────────────────┴─────────────────────────┴─────────────────────────┘
         */
    }

    @Test
    public void fdUCCStatement() {
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Dependant) AS Y " +
                "WHERE " +
                "FD(X,Y) " +
                "AND UCC(Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [DEPENDANT.csv."Key_UCC_2"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [DEPENDANT.csv."Key_UCC_1"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_1"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_2"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void fdUCCStatementCache() {
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Dependant) AS Y " +
                "WHERE " +
                "FD(X,Y) " +
                "AND UCC(Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [DEPENDANT.csv."Key_UCC_2"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [DEPENDANT.csv."Key_UCC_1"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_1"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_2"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void uccStatement(){
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "UCC(X) " +
                "AND UCC(Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [REFERENCED.csv."Key_UCC_1"]     │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [REFERENCED.csv."Key_UCC_2"]     │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │   [REFERENCED.csv."Key_NON_UCC_1",   │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │   REFERENCED.csv."Key_NON_UCC_2"]    │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void uccStatementCache(){
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "UCC(X) " +
                "AND UCC(Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [REFERENCED.csv."Key_UCC_1"]     │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [REFERENCED.csv."Key_UCC_2"]     │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │   [REFERENCED.csv."Key_NON_UCC_1",   │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │   REFERENCED.csv."Key_NON_UCC_2"]    │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void indStatement(){
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "IND(X,Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_2",      │     [REFERENCED.csv."Key_UCC_2",     │
         * │      DEPENDANT.csv."Key_UCC_1",       │     REFERENCED.csv."Key_UCC_1",      │
         * │    DEPENDANT.csv."Key_NON_UCC_1",     │   REFERENCED.csv."Key_NON_UCC_1",    │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │   REFERENCED.csv."Key_NON_UCC_2"]    │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void splitINDStatement(){
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y) " +
                "AND SPLIT(X,Y)";
        parse(sql);
        /**
         ┌───────────────────────────────────────┬──────────────────────────────────────┐
         │                   X                   │                  Y                   │
         ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         │      [Dependant.csv."Key_UCC_1",      │   [Referenced.csv."Key_NON_UCC_2",   │
         │      Dependant.csv."Key_UCC_2",       │   Referenced.csv."Key_NON_UCC_1",    │
         │    Dependant.csv."Key_NON_UCC_2",     │     Referenced.csv."Key_UCC_2",      │
         │    Dependant.csv."Key_NON_UCC_1"]     │     Referenced.csv."Key_UCC_1"]      │
         ├───────────────────────────────────────┼──────────────────────────────────────┤
         │   [Referenced.csv."Key_NON_UCC_2"]    │     [Dependant.csv."Key_UCC_2"]      │
         └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void nonsplitINDStatement(){
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(*) AS X, CC(*) AS Y " +
                "WHERE " +
                "IND(X,Y)";
        parse(sql);
        /**
         ┌───────────────────────────────────────┬──────────────────────────────────────┐
         │                   X                   │                  Y                   │
         ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         │      [Dependant.csv."Key_UCC_1",      │   [Referenced.csv."Key_NON_UCC_2",   │
         │      Dependant.csv."Key_UCC_2",       │   Referenced.csv."Key_NON_UCC_1",    │
         │    Dependant.csv."Key_NON_UCC_2",     │     Referenced.csv."Key_UCC_2",      │
         │    Dependant.csv."Key_NON_UCC_1"]     │     Referenced.csv."Key_UCC_1"]      │
         ├───────────────────────────────────────┼──────────────────────────────────────┤
         │   [Referenced.csv."Key_NON_UCC_1"]    │     [Referenced.csv."Key_UCC_1"]     │
         ├───────────────────────────────────────┼──────────────────────────────────────┤
         │   [Referenced.csv."Key_NON_UCC_2"]    │     [Dependant.csv."Key_UCC_2"]      │
         ├───────────────────────────────────────┼──────────────────────────────────────┤
         │   [Referenced.csv."Key_NON_UCC_2"]    │     [Referenced.csv."Key_UCC_2"]     │
         └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void indStatementCache(){
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "IND(X,Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_2",      │     [REFERENCED.csv."Key_UCC_2",     │
         * │      DEPENDANT.csv."Key_UCC_1",       │     REFERENCED.csv."Key_UCC_1",      │
         * │    DEPENDANT.csv."Key_NON_UCC_1",     │   REFERENCED.csv."Key_NON_UCC_1",    │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │   REFERENCED.csv."Key_NON_UCC_2"]    │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void fdStatement(){
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Dependant) AS Y " +
                "WHERE " +
                "FD(X, Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [DEPENDANT.csv."Key_UCC_2"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │   [DEPENDANT.csv."Key_NON_UCC_1"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │   [DEPENDANT.csv."Key_NON_UCC_2"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [DEPENDANT.csv."Key_UCC_1"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │   [DEPENDANT.csv."Key_NON_UCC_1"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │   [DEPENDANT.csv."Key_NON_UCC_2"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_1"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_2"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void fdStatementCache() {
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        EngineConfigurationSingleton.get().setCache(true);
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Dependant) AS Y " +
                "WHERE " +
                "FD(X, Y)";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [DEPENDANT.csv."Key_UCC_2"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │   [DEPENDANT.csv."Key_NON_UCC_1"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │   [DEPENDANT.csv."Key_NON_UCC_2"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [DEPENDANT.csv."Key_UCC_1"]      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │   [DEPENDANT.csv."Key_NON_UCC_1"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │   [DEPENDANT.csv."Key_NON_UCC_2"]    │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_1"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │    [DEPENDANT.csv."Key_NON_UCC_1",    │     [DEPENDANT.csv."Key_UCC_2"]      │
         * │    DEPENDANT.csv."Key_NON_UCC_2"]     │                                      │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void chainINDStatement() {
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        String sql = "SELECT " +
                "X, Y, Z " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y, CC(Dependant) AS Z " +
                "WHERE " +
                "IND(X, Y)";
        parse(sql);
    }

    @Test
    public void cardStatement() {
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "IND(X, Y) " +
                "AND UCC(X) " +
                "AND CARD(X) > 3";
        parse(sql);
        /**
         * ┌───────────────────────────────────────┬──────────────────────────────────────┐
         * │                   X                   │                  Y                   │
         * ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         * ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │     [REFERENCED.csv."Key_UCC_1"]     │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_1"]      │   [REFERENCED.csv."Key_NON_UCC_1"]   │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │     [REFERENCED.csv."Key_UCC_2"]     │
         * ├───────────────────────────────────────┼──────────────────────────────────────┤
         * │      [DEPENDANT.csv."Key_UCC_2"]      │   [REFERENCED.csv."Key_NON_UCC_2"]   │
         * └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void cardEqualStatement() {
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "IND(X, Y) " +
                "AND UCC(X) " +
                "AND CARD(X) = 3";
        parse(sql);
        /**
         ┌───────────────────────────────────────┬──────────────────────────────────────┐
         │                   X                   │                  Y                   │
         ├┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┼┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┬┤
         ├┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┼┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┴┤
         │    [DEPENDANT.csv."Key_NON_UCC_1",    │   [REFERENCED.csv."Key_NON_UCC_1",   │
         │    DEPENDANT.csv."Key_NON_UCC_2"]     │   REFERENCED.csv."Key_NON_UCC_2"]    │
         └───────────────────────────────────────┴──────────────────────────────────────┘
         */
    }

    @Test
    public void cardEmptyStatement() {
        InputConfigurationSingleton.get().setDATA_SET("TEST");
        String sql = "SELECT " +
                "X, Y " +
                "FROM CC(Dependant) AS X, CC(Referenced) AS Y " +
                "WHERE " +
                "IND(X, Y) " +
                "AND UCC(X) " +
                "AND CARD(X) > 4";
        parse(sql);
        /**
         [Empty table]
         */
    }

    private void parse(String sql) {
        Parser parser = new ANTLRParser();
        parser.parse(sql);
    }
}
