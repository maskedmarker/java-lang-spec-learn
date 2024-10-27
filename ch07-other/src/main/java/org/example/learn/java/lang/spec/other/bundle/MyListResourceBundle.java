package org.example.learn.java.lang.spec.other.bundle;

import java.util.ListResourceBundle;

public class MyListResourceBundle extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
                {"greeting", "Hello, World!"},
                {"farewell", "Goodbye, World!"}
        };
    }
}
