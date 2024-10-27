package org.example.learn.java.spec.commons.model.anno;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ScheduleAnnotationContainer {

    Schedule[] value();
}
