package org.example.learn.java.spec.commons.model.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@NonInheritableTrait(name = "不会被遗传的伤疤")
public @interface Scar {

    String name();

    String cause();
}
