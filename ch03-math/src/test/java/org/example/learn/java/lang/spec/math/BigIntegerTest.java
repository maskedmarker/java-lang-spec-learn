package org.example.learn.java.lang.spec.math;

import org.junit.Test;

import java.math.BigInteger;

public class BigIntegerTest {

    @Test
    public void test0() {
        BigInteger bigInteger = new BigInteger("1234567890");
        System.out.println("bigInteger = " + bigInteger);
    }
}
