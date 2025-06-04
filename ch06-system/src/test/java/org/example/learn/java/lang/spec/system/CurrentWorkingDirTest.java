package org.example.learn.java.lang.spec.system;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 获取当前jvm进程的工作目录
 */
public class CurrentWorkingDirTest {

    @Test
    public void test0() {
        String cwd = System.getProperty("user.dir");
        System.out.println("Paths.get(cwd).toAbsolutePath() = " + Paths.get(cwd).toAbsolutePath());
    }
}
