package org.example.learn.java.spec.commons.model.anno;

import java.lang.annotation.*;

/**
 * 不会被遗传的特征
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface NonInheritableTrait {

    String name();
}
