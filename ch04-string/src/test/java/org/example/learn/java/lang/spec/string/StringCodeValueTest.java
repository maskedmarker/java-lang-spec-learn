package org.example.learn.java.lang.spec.string;

import org.junit.Test;

public class StringCodeValueTest {

    @Test
    public void testStringCompare() {
        String str1 = "撒旦发顺丰";
        String str2 = "撒旦飞洒";
        int compare = str1.compareTo(str2);
        System.out.println("compare = " + compare);
    }

}
