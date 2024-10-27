package org.example.learn.java.lang.spec.system;

import org.junit.Test;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 系统变量
 */
public class SystemPropertyTest {

    @Test
    public void test0() {
        Properties properties = System.getProperties();
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            System.out.printf("%s=%s\n", entry.getKey(), entry.getValue());
        }
    }
}
