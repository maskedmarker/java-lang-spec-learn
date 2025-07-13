package org.example.learn.java.lang.spec.system;

import org.junit.Test;

import java.nio.file.Paths;

/**
 * 获取当前jvm可用的OS cpu数量
 */
public class CpuTest {

    @Test
    public void test0() {
        // Returns the number of processors available to the Java virtual machine.This value may change during a particular invocation of the virtual machine.
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("availableProcessors = " + availableProcessors);
    }
}
