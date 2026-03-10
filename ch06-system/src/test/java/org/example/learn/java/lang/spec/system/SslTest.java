package org.example.learn.java.lang.spec.system;

import org.junit.Test;

public class SslTest {

    @Test
    public void test0() {
        String ssl = System.getProperty("ssl");
        System.out.println("ssl = " + ssl);
    }
}
