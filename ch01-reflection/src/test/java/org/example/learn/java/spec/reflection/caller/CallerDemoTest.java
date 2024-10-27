package org.example.learn.java.spec.reflection.caller;

import org.junit.Test;

public class CallerDemoTest {

    @Test
    public void testGetCallerWithoutCallerSensitive() {
        CallerDemo demo = new CallerDemo();
        demo.demoCallerWithoutCallerSensitive();
    }

    @Test
    public void testGetCallerWithCallerSensitive() {
        CallerDemo demo = new CallerDemo();
        demo.demoCallerWithCallerSensitive();
    }
}