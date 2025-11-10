package org.example.learn.java.lang.spec.time;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class ToStringTest {

    @Test
    public void test0() {
        Calendar calendar = Calendar.getInstance();
        System.out.println("calendar = " + calendar);

        Date date = calendar.getTime();
        System.out.println("date = " + date);
    }
}
