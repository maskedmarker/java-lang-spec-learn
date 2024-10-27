package org.example.learn.java.lang.spec.string;

import org.junit.Test;

/**
 * @author cjh
 */
public class StringSubOperationTest {

    @Test
    public void test0() {
        String rawString = "0123456789abcdef";
        String subString = this.zeroLen(rawString);
        System.out.printf("substring is [%s]%n", subString);
    }

    private String zeroLen(String rawString) {
        return rawString.substring(0, 0);
    }
}
