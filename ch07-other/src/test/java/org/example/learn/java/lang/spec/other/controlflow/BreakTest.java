package org.example.learn.java.lang.spec.other.controlflow;

import org.junit.Test;

/**
 * Java 支持“标签”跳出多层嵌套循环
 */
public class BreakTest {

    @Test
    public void testNoException() {
        outer: for (int i = 0; i < 5; i++) {
            System.out.println("begin i = " + i);
            for (int j = 0; j < 5; j++) {
                System.out.println("j = " + j);
                if (j == 3) break outer;
            }
            System.out.println("end i = " + i);
        }
    }
}