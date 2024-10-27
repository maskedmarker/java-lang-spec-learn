package org.example.learn.java.spec.reflection.ann;

import org.example.learn.java.spec.commons.model.TaskPlan;
import org.example.learn.java.spec.commons.model.anno.Executor;
import org.example.learn.java.spec.commons.model.anno.Schedule;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 *
 * 所哟的注解都是interface,并且其父接口都是Annotation(Annotation也是一个接口)
 * 所有注解instance对应的类是由jdk动态代理机制自动创建的,而且这些代理类实现了注解接口
 *
 * 在class字节码文件中, 声明的注解(declared Annotations)其实是non-repeatable annotation和container annotation
 */
public class AnnotationInstanceTest {

    @Test
    public void test01() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        // 获取该方法上的注解instance
        Schedule[] schedules = method.getAnnotationsByType(Schedule.class);
        Arrays.stream(schedules).forEach(System.out::println);
        Executor[] executors = method.getAnnotationsByType(Executor.class);
        Arrays.stream(executors).forEach(System.out::println);
    }

    @Test
    public void test02() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        // 获取该方法上的所有注解instance,注意repeatable annotation返回的是container annotation
        // 在class字节码文件中, 声明的注解(declared Annotations)其实是@Executor和ScheduleAnnotationContainer
        Annotation[] annotation = method.getAnnotations();
        Arrays.stream(annotation).forEach(System.out::println);
        System.out.println("----------------------");

        // java.lang.reflect.Executable.getAnnotationsByType方法从所有declared Annotations中寻找直接/间接声明的Schedule类型的注解实例
        Schedule[] schedules = method.getAnnotationsByType(Schedule.class);
        Arrays.stream(schedules).forEach(System.out::println);
    }

    @Test
    public void test10() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        Annotation[] annotations = method.getAnnotations();

        // 所有注解instance对应的类是由jdk动态代理机制自动创建的
        Arrays.stream(annotations).forEach(annotation -> {
            Class<? extends Annotation> annotationClass = annotation.getClass();
            System.out.println(String.format("%s is of %s", annotation, annotationClass));
            System.out.println("----------------------");
        });
    }

    @Test
    public void test11() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        Annotation[] annotations = method.getAnnotations();

        // 所有注解instance对应的类是由jdk动态代理机制自动创建的,而且这些代理类实现了注解接口
        Arrays.stream(annotations).forEach(annotation -> {
            Class<? extends Annotation> annotationClass = annotation.getClass();
            System.out.println(String.format("%s is of %s", annotation, annotationClass));
            Class<?>[] interfaces = annotationClass.getInterfaces();
            Arrays.stream(interfaces).forEach(System.out::println);
            System.out.println("----------------------");
        });
    }

    @Test
    public void test12() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        Annotation[] annotations = method.getAnnotations();

        // 所有注解instance对应的类是由jdk动态代理机制自动创建的,而且这些代理类实现了注解接口,并且这些接口都是Annotation的子接口
        Arrays.stream(annotations).forEach(annotation -> {
            Class<? extends Annotation> annotationClass = annotation.getClass();
            System.out.println(String.format("%s is of %s", annotation, annotationClass));
            Class<?>[] interfaces = annotationClass.getInterfaces();
            Arrays.stream(interfaces).forEach(i -> {
                // 获取接口的父接口
                Class<?>[] parentInterfaces = i.getInterfaces();
                Arrays.stream(parentInterfaces).forEach(p -> {
                    System.out.println(String.format("%s is child of %s", i, p));
                });

            });
            System.out.println("----------------------");
        });
    }

    @Test
    public void test13() throws NoSuchMethodException {
        Method method = TaskPlan.class.getMethod("perform");
        Annotation[] annotations = method.getAnnotations();

        // 所有注解instance对应的类是由jdk动态代理机制自动创建的,而且这些代理类实现了注解接口,并且这些接口都是Annotation的子接口
        Arrays.stream(annotations).forEach(annotation -> {
            Class<? extends Annotation> annotationClass = annotation.getClass();
            System.out.println(String.format("%s is of %s", annotation, annotationClass));
            Class<?>[] interfaces = annotationClass.getInterfaces();
            Arrays.stream(interfaces).forEach(i -> {
                // 获取接口的父接口
                Class<?>[] parentInterfaces = i.getInterfaces();
                Arrays.stream(parentInterfaces).forEach(p -> {
                    System.out.println(String.format("%s is child of %s. and is interface? %b", i, p, p.isInterface()));
                });

            });
            System.out.println("----------------------");
        });
    }
}
