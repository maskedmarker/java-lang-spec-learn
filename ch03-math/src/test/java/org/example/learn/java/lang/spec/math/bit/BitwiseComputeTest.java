package org.example.learn.java.lang.spec.math.bit;

import org.junit.Test;

public class BitwiseComputeTest {

    @Test
    public void testSet() {
        int n = 0b0000_0101;
        System.out.println("Integer.toBinaryString(n) = " + Integer.toBinaryString(n));
        int t = BitwiseCompute.set(n, 1);
        System.out.println("Integer.toBinaryString(t) = " + Integer.toBinaryString(t));
    }

    @Test
    public void testClear() {
        int n = 0b0000_0101;
        System.out.println("Integer.toBinaryString(n) = " + Integer.toBinaryString(n));
        int t = BitwiseCompute.clear(n, 2);
        System.out.println("Integer.toBinaryString(t) = " + Integer.toBinaryString(t));
    }

    @Test
    public void testToggle() {
        int n = 0b0000_1111;
        System.out.println("Integer.toBinaryString(n) = " + Integer.toBinaryString(n));
        int t = BitwiseCompute.toggle(n, 2);
        System.out.println("Integer.toBinaryString(t) = " + Integer.toBinaryString(t));
    }


    @Test
    public void testRead() {
        int n = 0b0000_0101;
        System.out.println("Integer.toBinaryString(n) = " + Integer.toBinaryString(n));
        int t = BitwiseCompute.read(n, 2);
        System.out.println("Integer.toBinaryString(t) = " + Integer.toBinaryString(t));
    }
}
