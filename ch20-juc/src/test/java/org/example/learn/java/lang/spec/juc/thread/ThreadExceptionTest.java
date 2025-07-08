package org.example.learn.java.lang.spec.juc.thread;

import org.junit.Test;

/**
 * 注意多线程场景下,小心异常被吞了
 */
public class ThreadExceptionTest {

    @Test
    public void test() {
        new Thread(this::mustThrowsEx).start();
    }

    private void mustThrowsEx() {
        throw new RuntimeException("hi i throw an ex");
    }
}
