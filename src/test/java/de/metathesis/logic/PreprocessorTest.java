package de.metathesis.logic;

import de.metanome.algorithm_integration.input.InputIterationException;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.metathesis.Preprocessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

@DisplayName("Preprocessor Testing")
public class PreprocessorTest {

    private static Preprocessor preprocessor;

    @BeforeAll
    public static void setupNode() {
        InputConfigurationSingleton.get().setDATA_SET("WDC");
        InputConfigurationSingleton.get().setFILE_VALUE_SEPARATOR(",");

        preprocessor = Preprocessor.getInstance();
    }

    @Test
    public void testProduceSubSets() throws InputIterationException {
        BitSet superSet = new BitSet();
        superSet.set(1);
        superSet.set(7);
        superSet.set(8);
        superSet.set(4);

        List<BitSet> subsets = preprocessor.produceSubSets(superSet, 2);
        assert subsets.size() == 6;
    }
}
