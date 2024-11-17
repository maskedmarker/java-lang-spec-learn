package org.example.learn.java.lang.spec.math;

import org.junit.Test;

/**
 *
 */
public class BitMoveTest {

    @Test
    public void test0() {
        byte b1= 0b00000001;
        byte b2= 0b00000010;
        byte b3= 0b00000100;
        byte b4= 0b00001000;
        byte b5= 0b00010000;
        byte b6= 0b00100000;
        byte b7= 0b01000000;

        System.out.println("(b1 << 8) = " + (b1 << 8));
        System.out.println("(b2 << 8) = " + (b2 << 8));
        System.out.println("(b6 << 8) = " + (b6 << 8));
        System.out.println("(b7 << 8) = " + (b7 << 8));

        System.out.println("Integer.toBinaryString((b1 << 8)) = " + Integer.toBinaryString((b1 << 8)));
        System.out.println("Integer.toBinaryString((b2 << 8)) = " + Integer.toBinaryString((b2 << 8)));
        System.out.println("Integer.toBinaryString((b6 << 8)) = " + Integer.toBinaryString((b6 << 8)));
        System.out.println("Integer.toBinaryString((b7 << 8)) = " + Integer.toBinaryString((b7 << 8)));

        // Shift operation '<<' by overly large constant value 32
        System.out.println("Integer.toBinaryString((b1 << 32)) = " + Integer.toBinaryString((b1 << 32)));
        System.out.println("Integer.toBinaryString((b1 << 32)) = " + Integer.toBinaryString(((b1 << 31) << 3)));
    }
}
