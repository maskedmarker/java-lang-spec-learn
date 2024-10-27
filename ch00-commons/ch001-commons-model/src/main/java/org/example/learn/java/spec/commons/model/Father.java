package org.example.learn.java.spec.commons.model;

import org.example.learn.java.spec.commons.model.anno.Handsome;
import org.example.learn.java.spec.commons.model.anno.InheritableTrait;
import org.example.learn.java.spec.commons.model.anno.NonInheritableTrait;
import org.example.learn.java.spec.commons.model.anno.Scar;

import java.util.StringJoiner;

@InheritableTrait(name = "身高")
@NonInheritableTrait(name = "低度近视")
@Scar(name = "腿伤的伤疤", cause = "踢球时留下的")
@Handsome(level = "非常帅气")
public class Father {

    private String name;

    public Father(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Father.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .toString();
    }
}
