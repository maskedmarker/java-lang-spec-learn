package org.example.learn.java.lang.spec.io.net.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.Selector;

public class ReflectionUtilsTest {

    @Test
    public void test0() throws ClassNotFoundException {
        Class<?> aClass = Class.forName("sun.nio.ch.WindowsSelectorImpl");
        Field wakeupPipe = ReflectionUtils.getDeclaredField(aClass, "wakeupPipe");
        Assert.assertNotNull(wakeupPipe);
    }

    @Test
    public void test1() throws ClassNotFoundException, IOException, IllegalAccessException {
        Selector selector = Selector.open();
        Class<? extends Selector> aClass = selector.getClass();
        Assert.assertEquals(Class.forName("sun.nio.ch.WindowsSelectorImpl"), aClass);
        Field wakeupPipe = ReflectionUtils.getDeclaredField(aClass, "wakeupPipe");
        Assert.assertNotNull(wakeupPipe);

        wakeupPipe.setAccessible(true);
        Object value = wakeupPipe.get(selector);
        System.out.println("value = " + value);
    }
}
