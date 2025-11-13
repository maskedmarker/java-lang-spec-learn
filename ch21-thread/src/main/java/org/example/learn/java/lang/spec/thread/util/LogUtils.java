package org.example.learn.java.lang.spec.thread.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

public class LogUtils {

    public static void log(String logFormat, Object... args) {
        Date now = Calendar.getInstance().getTime();
        Formatter formatter = new Formatter();
        formatter.format("[%s] [%s]: ", new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS").format(now), Thread.currentThread().getName());
        formatter.format(logFormat, args);
        String log = formatter.toString();
        System.out.println(log);
    }
}
