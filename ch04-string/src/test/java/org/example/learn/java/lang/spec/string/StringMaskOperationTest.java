package org.example.learn.java.lang.spec.string;

import org.junit.Test;

public class StringMaskOperationTest {

    @Test
    public void test0() {
        String rawString = "0123456789a";
        String maskString = "******************************";
        String maskedString = this.doMaskString(rawString, 11, maskString);
        System.out.printf("maskedString is [%s], length is %d%n", maskedString, maskedString.length());
    }

    /**
     * mask the raw string with maskString
     * @param rawString the raw string
     * @param offset offset position of the raw string
     * @return masked string
     * @exception  NullPointerException if raw string is null or empty
     * @exception  IllegalArgumentException if offset can not be negative or
     *                                      offset is equal or greater than raw string length
     */
    private String doMaskString(String rawString, int offset, String maskString) {
        if (rawString == null || rawString.length() == 0) {
            throw new NullPointerException("raw string is null or empty");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("offset can not be negative");
        }

        if (offset >= rawString.length()) {
            throw new IllegalArgumentException("offset can not be equal or greater than raw string length");
        }

        int len = Math.min(rawString.length() - offset, maskString.length());
        return rawString.substring(0, offset)
                + maskString.substring(0, len)
                + rawString.substring(offset + len);
    }
}
