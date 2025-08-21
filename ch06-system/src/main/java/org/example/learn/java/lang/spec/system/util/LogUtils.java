package org.example.learn.java.lang.spec.system.util;

import java.util.Map;

public class LogUtils {

    public static void prettyLog(Map<Object, Object> map) {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            System.out.printf("%s=%s\n", entry.getKey(), entry.getValue());
        }
    }
}
