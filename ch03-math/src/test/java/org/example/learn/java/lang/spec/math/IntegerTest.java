package org.example.learn.java.lang.spec.math;

import org.junit.Test;

public class IntegerTest {

    /**
     * reverse integer的字节序列
     */
    @Test
    public void test01() {
        int i = 0x12_34_56_78;
        System.out.println("Integer.toHexString(i) = " + Integer.toHexString(i));
        System.out.println("Integer.reverseBytes(i) = " + Integer.toHexString(Integer.reverseBytes(i)));
    }
}
