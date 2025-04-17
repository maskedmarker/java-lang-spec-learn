package org.example.learn.java.lang.spec.string;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * 字符串拼接
 */
public class StringConcatenationTest {

    @Test
    public void test0() {
        List<String> strs = Arrays.asList("zhangsan", "lisi", "wangwu");
        StringJoiner stringJoiner = new StringJoiner(",", "(", ")");
        strs.forEach(str -> stringJoiner.add(str));
        System.out.println("stringJoiner.toString() = " + stringJoiner.toString());
    }


}
