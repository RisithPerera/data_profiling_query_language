package de.metaserve.e2e;

import de.metaserve.engine.Metaserve;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.singletons.EngineConfigurationSingleton;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryTests {
    Metaserve metaserve;

    static Stream<Arguments> testCaseArguments() {
        return Stream.of(
                Arguments.of("IND", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y)", 1000),
                Arguments.of("FD", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y)", 1000),
                Arguments.of("UCC", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X WHERE UCC(X)", 1000),
                Arguments.of("Foreign-Key", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y)", 1000),
                Arguments.of("Foreign-Key 2", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND SPLIT(X,Y) AND CARDINALITY(X) > 5", 1000),
                Arguments.of("Foreign-Key 3", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND SPLIT(X,Y) AND CARDINALITY(X) > 10", 1000),
                Arguments.of("Foreign-Key 4", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND CARDINALITY(X) > 0", 1000),
                Arguments.of("Chain", true, "TPCHNEW",
                        "SELECT X, Y, Z FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE IND(X,Y) AND IND(Y,Z)", 1000),
                Arguments.of("Chain 2", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND IND(Y,X) AND CARDINALITY(X) > 0", 1000),
                Arguments.of("Chain 3", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND FD(Y,X)", 1000),
                Arguments.of("Mix", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND IND(X,Y)", 1000),
                Arguments.of("Not FD", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND NOT UCC(X)", 1000),
                Arguments.of("Chain", true, "TPCHNEW",
                        "SELECT X, Y, Z, A FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS A WHERE IND(X,Y) AND IND(Y,Z) AND IND(Z,A) AND CARDINALITY(X) > 0", 1000),
                Arguments.of("Mix 2", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE FD(X,Y) AND IND(Y,X) AND NOT UCC(X)", 1000),
                Arguments.of("Mix 3", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z WHERE FD(X,Y) AND IND(Z,Y)", 1000),
                Arguments.of("Mix 3", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND SPLIT(X,Y)", 1000),
                Arguments.of("Mix 3", true, "TPCHNEW",
                        "SELECT X, Y, Z, A FROM CC(*) AS X, CC(*) AS Y, CC(*) AS Z, CC(*) AS A WHERE IND(X,Y) AND FD(Y,Z) AND FD(X,A) AND NOT UCC(X) AND NOT UCC(Y)", 1000),
                Arguments.of("NOT Operator", true, "TPCHNEW",
                        "SELECT X, Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND NOT UCC(X) AND SPLIT(X,Y) AND CARDINALITY(X) > 2", 12),
                Arguments.of("Unknown Bug", true, "TPCHNEW",
                        "SELECT X,Y FROM CC(*) AS X, CC(*) AS Y WHERE IND(X,Y) AND UCC(Y) AND SPLIT(X,Y) AND CARDINALITY(X) > 7", 1315),
                Arguments.of("Three select elements", true, "Test",
                        "SELECT X, Y, Z FROM CC(Dependant) AS X, CC(Referenced) AS Y, CC(Dependant) AS Z WHERE IND(X,Y) AND UCC(X) AND UCC(Z) AND SPLIT(X,Y) AND SIZE(X) <= 2", 5),
                Arguments.of("Three select elements", false, "Test",
                        "SELECT X, Y, Z FROM CC(Dependant) AS X, CC(Referenced) AS Y, CC(Dependant) AS Z WHERE IND(X,Y) AND UCC(X) AND UCC(Z) AND SPLIT(X,Y) AND SIZE(X) <= 2", 5)
                );
    }


    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void shortQueryTest(String name, boolean cache, String dataSet, String query, int expectedResultSize) {
        metaserve = new Metaserve();
        EngineConfigurationSingleton.get().setLog(false).setCache(cache).getInputConfig().setDATA_SET(dataSet);;

        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        if(resultSetList.isEmpty())
            assertEquals(expectedResultSize, 0);
        else
            assertEquals(expectedResultSize, resultSetList.get(0).size(), name + ": Expected " + expectedResultSize + " but got " + resultSetList.get(0).size());
    }

    //All queries are executed cached and non cached
    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void longQueryTest(String name, boolean cache, String dataSet, String query, int expectedResultSize) {
        for (int i = 0; i < 2; i++) {
            metaserve = new Metaserve();

            EngineConfigurationSingleton.get().setLog(false).setCache(i == 0).getInputConfig().setDATA_SET(dataSet);

            List<ResultSet> resultSetList = metaserve.executeQuery(query);
            if(resultSetList.isEmpty())
                assertEquals(expectedResultSize, 0);
            else
                assertEquals(expectedResultSize, resultSetList.size(), name + ": Expected " + expectedResultSize + " but got " + resultSetList.size());
        }
    }
}
