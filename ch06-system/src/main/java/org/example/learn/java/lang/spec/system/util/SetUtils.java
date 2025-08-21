package org.example.learn.java.lang.spec.system.util;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SetUtils {

    /** 交集 */
    public static Map<Object, Object> intersect(Map<Object, Object> map1, Map<Object, Object> map2) {
        return map1.entrySet().stream()
                .filter(e -> map2.containsKey(e.getKey()) && Objects.equals(e.getValue(), map2.get(e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** 差集 */
    public static Map<Object, Object> subtract(Map<Object, Object> map1, Map<Object, Object> map2) {
        return map1.entrySet().stream()
                .filter(e -> !map2.containsKey(e.getKey()) || !Objects.equals(e.getValue(), map2.get(e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
