package org.example.learn.java.lang.spec.other.format;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class TimeFormatTest {

    @Test
    public void defaultFormat() {
        Date now = Calendar.getInstance().getTime();
        System.out.println("now = " + now);
    }
}
