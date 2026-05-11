package de.metathesis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripleTest_03 extends BaseTest{

    @Test
    @Order(0)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X)");
    }

    @Test
    @Order(1)
    public void testT14() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(2)
    public void testT15() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(3)
    public void testT16() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Y,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(4)
    public void testT17() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,Y) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(5)
    public void testT18() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(6)
    public void testT19() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(7)
    public void testT20() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Y,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(8)
    public void testT21() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,Y) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(9)
    public void testT22() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Z)");
    }

    @Test
    @Order(10)
    public void testT23() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(Z)");
    }

    @Test
    @Order(11)
    public void testT24() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Y,Z) AND IND(X,Y) AND UCC(Z)");
    }

    @Test
    @Order(12)
    public void testT25() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,Y) AND IND(X,Y) AND UCC(Z)");
    }
}
