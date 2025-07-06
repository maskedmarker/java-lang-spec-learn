package org.example.learn.java.lang.spec.string;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * 标准(即全球通用的)字符编码
 */
public class StandardCharsetTest {


    @Test
    public void test01() {
        System.out.println("StandardCharsets.UTF_8 = " + StandardCharsets.UTF_8);
        System.out.println("StandardCharsets.ISO_8859_1 = " + StandardCharsets.ISO_8859_1);
    }
}
