package de.metathesis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripleTest_02 extends BaseTest{

    @Test
    @Order(0)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X)");
    }

    @Test
    @Order(1)
    public void testT6() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(2)
    public void testT7() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(3)
    public void testT8() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Y,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(4)
    public void testT9() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,Y) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(5)
    public void testT10() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(6)
    public void testT11() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,X) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(7)
    public void testT12() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Y,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(8)
    public void testT13() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,Y) AND IND(X,Y) AND UCC(X)");
    }
}
