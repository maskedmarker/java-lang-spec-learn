package org.example.learn.java.spec.commons.model;

import org.example.learn.java.spec.commons.model.anno.Handsome;

import java.util.StringJoiner;

@Handsome(level = "还行")
public class Son extends Father {

    public Son(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Son.class.getSimpleName() + "[", "]")
                .toString();
    }
}
