package org.example.learn.java.lang.spec.system;

import org.example.learn.java.lang.spec.system.util.LogUtils;
import org.example.learn.java.lang.spec.system.util.SetUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统变量
 *
 * System.getenv()具体指的是操作系统的环境变量,只读不能修改
 * System.getProperties()具体指的是JVM的系统属性,可以在运行时修改
 */
public class SystemPropertyTest {

    @Test
    public void test0() {
        Properties properties = System.getProperties();
        LogUtils.prettyLog(properties);
    }

    @Test
    public void test1() {
        Map<String, String> env = System.getenv();
        Properties properties = System.getProperties();
        System.out.println("-----------------------------/// 数量比对 ///------------------------------------");
        System.out.println("count of env var is " + env.size());
        System.out.println("count of system properties is " + properties.size());
        System.out.println("-----------------------------------------------------------------");

        System.out.println("-----------------------------/// env与system properties同时都有 ///------------------------------------");
        Map<Object, Object> intersect = SetUtils.intersect(new HashMap<>(env), properties);
        System.out.println("intersect.size() = " + intersect.size());
        LogUtils.prettyLog(intersect);
        System.out.println("-----------------------------------------------------------------");

        System.out.println("-----------------------------/// env中有,system properties没有 ///------------------------------------");
        Map<Object, Object> subtract1 = SetUtils.subtract(new HashMap<>(env), properties);
        LogUtils.prettyLog(subtract1);
        System.out.println("-----------------------------------------------------------------");

        System.out.println("-----------------------------/// env中没有,system properties有 ///------------------------------------");
        Map<Object, Object> subtract2 = SetUtils.subtract(new HashMap<>(env), properties);
        LogUtils.prettyLog(subtract2);
        System.out.println("-----------------------------------------------------------------");
    }

    @Test
    public void test2() {
        Map<String, String> env = System.getenv();
        Properties properties = System.getProperties();
        System.out.println("count of env var is " + env.size());
        System.out.println("count of system properties is " + properties.size());

        Map<Object, Object> intersect = SetUtils.intersect(new HashMap<>(env), properties);
        System.out.println("intersect.size() = " + intersect.size());
        LogUtils.prettyLog(intersect);

        // 操作系统环境变量和jvm的系统属性通常情况下是没有交集的,因为隶属不同的范畴
        Assert.assertEquals(0, intersect.size());
    }
}
