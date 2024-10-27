package org.example.learn.java.spec.reflection.ann;

import org.example.learn.java.spec.commons.model.TaskPlan;
import org.example.learn.java.spec.commons.model.anno.Schedule;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

public class RepeatableAnnotationTest {

    @Test
    public void test0() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        // 获取该方法上的@Schedule注解instance
        Schedule[] annotation = method.getAnnotationsByType(Schedule.class);
        Arrays.stream(annotation).forEach(System.out::println);
    }

    @Test
    public void test1() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        // 获取该方法上的@Schedule注解instance
        Schedule[] annotation = method.getAnnotationsByType(Schedule.class);
        Arrays.stream(annotation).forEach(System.out::println);
    }
}
