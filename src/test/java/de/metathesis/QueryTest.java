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
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testIND() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testFD() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ1() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ2() {
        String query = "SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ3() {
        String query = "SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND FD(Y,W)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ4() {
        String query = "SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X) AND IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ5() {
        String query = "SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND IND(Y,Z) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ6() {
        String query = "SELECT P, Q, X, Y FROM CC(*) P, CC(*) Q, CC(*) X, CC(*) Y WHERE FD(X,P) AND IND(X,Y) AND FD(Y,Q)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ7() {
        String query = "SELECT P, Q, X, Y FROM CC(*) P, CC(*) Q, CC(*) X, CC(*) Y WHERE FD(P,X) AND IND(X,Y) AND FD(Q,Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ8() {
        String query = "SELECT X, Y, Z, P, Q, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) Q, CC(*) R " +
                "WHERE FD(X,P) AND FD(X,Q) AND IND(X,Y) AND IND(Y,Z) AND FD(Y,R) AND UCC(R)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ9() {
        String query = "SELECT X, Y, Z, P, Q, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) Q, CC(*) R " +
                "WHERE FD(X,Y) AND FD(Y,Z) AND IND(Z,R) AND IND(X,P) AND IND(X,Q) AND UCC(Q)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testQ10() {
        String query = "SELECT X, Y, Z, P, Q, R FROM CC(*) X, CC(*) Y, CC(*) Z, CC(*) P, CC(*) Q, CC(*) R " +
                "WHERE IND(X,Z) AND FD(Z,P) AND FD(Z,Q) AND IND(X,Y) AND IND(Y,R) AND UCC(R)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }

    @Test
    public void testExtra() {
        String query = "SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE UCC(Z) AND FD(Z,X) AND IND(X,Y) AND UCC(Y)";
        List<ResultSet> resultSetList = metaserve.executeQuery(query);
        ResultSet first = resultSetList.getFirst();
        first.printResults();
    }
}
