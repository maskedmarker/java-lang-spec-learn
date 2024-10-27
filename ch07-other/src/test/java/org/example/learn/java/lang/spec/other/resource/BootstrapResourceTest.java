package org.example.learn.java.lang.spec.other.resource;

import org.junit.Test;
import sun.misc.URLClassPath;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BootstrapResourceTest {

    @Test
    public void test0() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        Method getBootstrapClassPathMethod = ClassLoader.class.getDeclaredMethod("getBootstrapClassPath");
        getBootstrapClassPathMethod.setAccessible(true);
        URLClassPath ucp  = (URLClassPath) getBootstrapClassPathMethod.invoke(systemClassLoader);
        System.out.println("ucp = " + ucp);

        Field loadersField = URLClassPath.class.getDeclaredField("loaders");
        loadersField.setAccessible(true);
        Object o = loadersField.get(ucp);
        System.out.println("o = " + o);
    }

    @Test
    public void test1() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        Method method = systemClassLoader.getClass().getDeclaredMethod("getBootstrapClassPath");
        method.setAccessible(true);
        URLClassPath ucp  = (URLClassPath) method.invoke(systemClassLoader);;
        System.out.println("ucp = " + ucp);
    }
}
