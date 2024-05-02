package de.metaserve.e2e;
import de.metaserve.engine.Metaserve;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionTest {

    Metaserve metaserve;
    static Stream<Arguments> testCaseArguments() {
        return Stream.of(
                Arguments.of("Exception Test 1", true, "TPCHNEW", "SELECT * FROM Nation AS X", RuntimeException.class),
                Arguments.of("Exception Test 2", true, "TPCHNEW", "SELECT * FROM *", RuntimeException.class)
        );
    }

    // Create a parameterized test method
    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void testExceptionScenario(String name, boolean cache, String dataSet, String query, Class<? extends Exception> expectedException) {
        metaserve = new Metaserve();
        EngineConfigurationSingleton.get().setLog(false).setCache(cache).getInputConfig().setDATA_SET(dataSet);
        assertThrows(expectedException, () -> metaserve.executeQuery(query)
                , name + ": Expected " + expectedException.getSimpleName() + " but it wasn't thrown.");
    }
}

