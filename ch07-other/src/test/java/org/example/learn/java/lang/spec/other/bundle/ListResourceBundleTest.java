package org.example.learn.java.lang.spec.other.bundle;

import org.junit.Test;

import java.util.Locale;
import java.util.ResourceBundle;

public class ListResourceBundleTest {

    @Test
    public void test0() {
        ResourceBundle bundle = ResourceBundle.getBundle(MyListResourceBundle.class.getName());

        // 获取资源
        String greeting = bundle.getString("greeting");
        String farewell = bundle.getString("farewell");

        System.out.println(greeting);
        System.out.println(farewell);
    }

    /**
     * 指定locale
     */
    @Test
    public void test1() {
        ResourceBundle bundle = ResourceBundle.getBundle(MyListResourceBundle.class.getName(), Locale.FRENCH);

        // 获取资源
        String greeting = bundle.getString("greeting");
        String farewell = bundle.getString("farewell");

        System.out.println(greeting);
        System.out.println(farewell);
    }

    /**
     * 如果找不到对应locale的资源,就会使用basename对应的资源
     * 当不指定locale时,使用jvm基于os信息获取到的默认locale
     */
    @Test
    public void test2() {
        ResourceBundle bundle = ResourceBundle.getBundle(MyListResourceBundle.class.getName(), Locale.CHINA);

        // 获取资源
        String greeting = bundle.getString("greeting");
        String farewell = bundle.getString("farewell");

        System.out.println(greeting);
        System.out.println(farewell);
    }
}
