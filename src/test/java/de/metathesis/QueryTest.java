package de.metathesis;

import de.metaserve.engine.Metaserve;
import de.metaserve.util.listener.ComplitionListener;
import de.metaserve.util.result.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class QueryTest {
    Metaserve metaserve;

    @BeforeEach
    public void setup(){
        metaserve = new Metaserve();
        metaserve.addListener((ComplitionListener) (query, resultSet, totalTime, resultSize) -> {
            System.out.println("#Dependencies: " + query.getMetaData().getNumberOfReduction());
            System.out.println("#Dependency Map" + query.getMetaData().getMap());
            System.out.println("#Candidates: " + query.getMetaData().getNumberOfCandidates());
            System.out.println("#Rows: " + resultSize);
        });
    }

    @Test
    public void testUCC() {
        String query = "SELECT X FROM CC(*) X WHERE UCC(X)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.get(0);
        first.printResults();
    }

    @Test
    public void testIND() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.get(0);
        first.printResults();
    }

    @Test
    public void testFD() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.get(0);
        first.printResults();
    }

    @Test
    public void testQ1() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.get(0);
        first.printResults();
    }

    @Test
    public void testQ2() {
        String query = "SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.get(0);
        first.printResults();
    }

    @Test
    public void testExtra() {
        String query = "SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE UCC(Z) AND FD(X,Z) AND IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        System.out.println(resultSetList.getFirst().getRows2());
    }
}
