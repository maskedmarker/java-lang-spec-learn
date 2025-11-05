package org.example.learn.java.lang.spec.math;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class DecimalCastTest {


    /**
     * long cast to int 直接截断
     * 截断后的二进制按补码处理
     */
    @Test
    public void test0() {
        long l = 0xffff_ffff_efff_ffffL;
        int i = (int) l;
        System.out.println("Integer.toBinaryString(i) = " + Integer.toHexString(i));
        Assert.assertEquals("long cast to int 直接截断,仅仅保留低32-bit", 0xefff_ffff, i);

        l = 0xffff_ffff_ffff_ffffL;
        i = (int) l;
        System.out.println("i = " + i);
        System.out.println("Integer.toBinaryString(i) = " + Integer.toHexString(i));
        Assert.assertTrue("保留的低32-bit按补码处理,最高位是1的就是负数", i < 0);
    }

    
}
