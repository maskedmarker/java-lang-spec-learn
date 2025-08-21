package org.example.learn.java.lang.spec.system;

import org.example.learn.java.lang.spec.system.util.SetUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SetUtilsTest {

    @Test
    public void test0(){
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("A", 1);
        map1.put("B", 2);
        map1.put("C", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("B", 2);
        map2.put("C", 30);
        map2.put("D", 4);

        Map<Object, Object> subtract = SetUtils.subtract(new HashMap<>(map1), new HashMap<>(map2));
        System.out.println("subtract = " + subtract);
    }

    @Test
    public void test1(){
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("A", 1);
        map1.put("B", 2);
        map1.put("C", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("B", 2);
        map2.put("C", 30);
        map2.put("D", 4);

        Map<Object, Object> subtract = SetUtils.intersect(new HashMap<>(map1), new HashMap<>(map2));
        System.out.println("subtract = " + subtract);
    }
}
