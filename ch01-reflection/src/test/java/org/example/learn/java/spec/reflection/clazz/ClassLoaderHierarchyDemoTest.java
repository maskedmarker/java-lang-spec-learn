package org.example.learn.java.spec.reflection.clazz;

import org.junit.Test;

import java.sql.DriverManager;
import java.util.ArrayList;

public class ClassLoaderHierarchyDemoTest {

    private ClassLoaderHierarchyDemo demo = new ClassLoaderHierarchyDemo();

    @Test
    public void testClassLoaders() throws ClassNotFoundException {
        System.out.println("Classloader of this class:" + ClassLoaderHierarchyDemoTest.class.getClassLoader());
        System.out.println("Classloader of DriverManager:" + DriverManager.class.getClassLoader());
        System.out.println("Classloader of ArrayList:" + ArrayList.class.getClassLoader());
    }

    @Test
    public void testDisplayBuiltInClassLoaders() {
        demo.displayBuiltInClassLoaders();
    }

    @Test
    public void testSystemClassLoader() {
        demo.systemClassLoader();
    }
}