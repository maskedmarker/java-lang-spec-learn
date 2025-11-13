package org.example.learn.java.lang.spec.other.format;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

public class TimeFormatTest {

    @Test
    public void defaultFormat() {
        Date now = Calendar.getInstance().getTime();
        System.out.println("now = " + now);
    }

    @Test
    public void test01() {
        Date now = Calendar.getInstance().getTime();
        String str = new SimpleDateFormat("yyyyMMdd HH:mm:ss:SSS").format(now);
        System.out.println("str = " + str);
    }
}
