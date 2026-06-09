package de.metathesis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullTest extends BaseTest{

    @Test
    @Order(0)
    public void warmUp() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE UCC(X)");
    }

    @Test
    @Order(1)
    public void testU1() {
        runQuery("SELECT X FROM CC(*) X WHERE UCC(X)");
    }

    @Test
    @Order(2)
    public void testU2() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE FD(X,Y)");
    }

    @Test
    @Order(3)
    public void testU3() {
        runQuery("SELECT X, Y FROM CC(*) X, CC(*) Y WHERE IND(X,Y)");
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

    @Test
    @Order(12)
    public void testB9() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND IND(X,Z)");
    }

    @Test
    @Order(13)
    public void testB10() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Y) AND IND(Y,Z)");
    }

    @Test
    @Order(14)
    public void testT1() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE UCC(X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(15)
    public void testT2() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,W) AND IND(X,Y) AND FD(Y,Z)");
    }

    @Test
    @Order(16)
    public void testT3() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(W,X) AND IND(X,Y) AND FD(Z,Y)");
    }

    @Test
    @Order(17)
    public void testT4() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,W) AND IND(X,Y) AND FD(Z,Y)");
    }

    @Test
    @Order(18)
    public void testT5() {
        runQuery("SELECT W, X, Y, Z FROM CC(*) W, CC(*) X, CC(*) Y, CC(*) Z WHERE FD(W,X) AND IND(X,Y) AND FD(Y,Z)");
    }

    @Test
    @Order(19)
    public void testT6() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(20)
    public void testT7() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(21)
    public void testT8() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Y,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(22)
    public void testT9() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,Y) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(23)
    public void testT10() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(X,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(24)
    public void testT11() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,X) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(25)
    public void testT12() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Y,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(26)
    public void testT13() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE IND(Z,Y) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(27)
    public void testT14() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(28)
    public void testT15() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(29)
    public void testT16() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Y,Z) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(30)
    public void testT17() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,Y) AND IND(X,Y) AND UCC(Y)");
    }

    @Test
    @Order(31)
    public void testT18() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(32)
    public void testT19() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(33)
    public void testT20() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Y,Z) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(34)
    public void testT21() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,Y) AND IND(X,Y) AND UCC(X)");
    }

    @Test
    @Order(35)
    public void testT22() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(X,Z) AND IND(X,Y) AND UCC(Z)");
    }

    @Test
    @Order(36)
    public void testT23() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,X) AND IND(X,Y) AND UCC(Z)");
    }

    @Test
    @Order(37)
    public void testT24() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Y,Z) AND IND(X,Y) AND UCC(Z)");
    }

    @Test
    @Order(38)
    public void testT25() {
        runQuery("SELECT X, Y, Z FROM CC(*) X, CC(*) Y, CC(*) Z WHERE FD(Z,Y) AND IND(X,Y) AND UCC(Z)");
    }
}
