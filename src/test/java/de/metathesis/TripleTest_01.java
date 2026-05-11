package de.metathesis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripleTest_01 extends BaseTest{
    @Test
    @Order(0)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X)");
    }

    @Test
    @Order(1)
    public void testT1() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE UCC(X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(2)
    public void testT2() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,W) AND IND(X,Y) AND FD(Y,Z)");
    }

    @Test
    @Order(3)
    public void testT3() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(W,X) AND IND(X,Y) AND FD(Z,Y)");
    }

    @Test
    @Order(4)
    public void testT4() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,W) AND IND(X,Y) AND FD(Z,Y)");
    }

    @Test
    @Order(5)
    public void testT5() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(W,X) AND IND(X,Y) AND FD(Y,Z)");
    }

}
