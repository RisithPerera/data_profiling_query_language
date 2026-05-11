package de.metathesis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BinaryTest extends BaseTest{

    @Test
    @Order(0)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X)");
    }

    @Test
    @Order(4)
    public void testB1() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(5)
    public void testB2() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y) AND UCC(X)");
    }

    @Test
    @Order(6)
    public void testB3() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(7)
    public void testB4() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(8)
    public void testB5() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(Y,Z)");
    }

    @Test
    @Order(9)
    public void testB6() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(X,Z)");
    }

    @Test
    @Order(10)
    public void testB7() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(Z,Y)");
    }

    @Test
    @Order(11)
    public void testB8() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND FD(Z,X)");
    }
}
