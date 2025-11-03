package org.example.learn.java.lang.spec.math;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class DecimalCastTest {


    /**
     * long cast to int 直接截断
     */
    @Test
    public void test0() {
        long l = 0xffff_ffff_efff_ffffL;
        int i = (int) l;
        System.out.println("Integer.toBinaryString(i) = " + Integer.toHexString(i));
        Assert.assertEquals(0xefff_ffff, i);
    }

    
}
