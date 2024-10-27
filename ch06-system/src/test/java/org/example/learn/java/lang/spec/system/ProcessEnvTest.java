package org.example.learn.java.lang.spec.system;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 * 进程环境变量
 */
public class ProcessEnvTest {

    @Test
    public void test0() {
        Map<String, String> env = System.getenv();
        Set<Map.Entry<String, String>> entries = env.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
    }
}
