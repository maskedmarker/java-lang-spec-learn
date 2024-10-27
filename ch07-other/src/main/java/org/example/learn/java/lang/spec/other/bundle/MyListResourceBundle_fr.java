package org.example.learn.java.lang.spec.other.bundle;

import java.util.ListResourceBundle;

public class MyListResourceBundle_fr extends ListResourceBundle {

    @Override
    protected Object[][] getContents() {
        return new Object[][] {
                {"greeting", "Bonjour, le monde!"},
                {"farewell", "Au revoir, le monde!"}
        };
    }
}
