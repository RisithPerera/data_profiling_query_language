package de.metathesis;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UnaryTest extends BaseTest{

    @Test
    @Order(0)
    public void warmUp() {
        runQuery("SELECT X FROM CC(*) X WHERE UCC(X)");
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
}
