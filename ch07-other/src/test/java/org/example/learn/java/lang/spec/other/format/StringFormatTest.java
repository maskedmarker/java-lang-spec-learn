package org.example.learn.java.lang.spec.other.format;

import org.junit.Test;

import java.util.Calendar;
import java.util.Formatter;

public class StringFormatTest {


    @Test
    public void test0() {
        Formatter formatter = new Formatter();
        Formatter log = formatter.format("thread[%s] is logging at [%s]", Thread.currentThread().getName(), Calendar.getInstance().getTime());
        System.out.println("log = " + log);
    }
}
