package de.metaserve.engine;


import de.metaserve.util.singletons.InputConfigurationSingleton;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class EngineTest {
    String[] files;
    int size;
    String query;

    public EngineTest(String query, String dataSetName, String[] files, int resultSize){
        this.files = files;
        this.size = resultSize;
        this.query = query;
        InputConfigurationSingleton.get().setDATA_SET(dataSetName);
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                    "SELECT X FROM CC(*) AS X WHERE IND(X)",
                    "Test",
                    new String[]{"Dependant", "Referenced", "Ref"},
                    8
                },
                {
                    "SELECT X FROM CC(*) AS X WHERE IND(X)",
                    "TPCH",
                    new String[]{"customer", "lineitem", "nation", "orders", "part", "region", "supplier"},
                    20
                }
        });
    }

    @Before
    public void setUp() {
        InputConfigurationSingleton.get().setDATA_SET("Test");
    }

    @Test
    public void execute(){

    }


}
