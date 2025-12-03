package org.example.learn.java.lang.spec.juc.util;

import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    public static void yieldWait(long timeout, TimeUnit timeUnit) {
        long end = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (System.currentTimeMillis() < end) {
            Thread.yield();
        }
    }
}
