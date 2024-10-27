package org.example.learn.java.spec.reflection.ann;

import org.example.learn.java.spec.commons.model.Father;
import org.example.learn.java.spec.commons.model.Son;
import org.example.learn.java.spec.commons.model.anno.Handsome;
import org.example.learn.java.spec.commons.model.anno.InheritableTrait;
import org.example.learn.java.spec.commons.model.anno.NonInheritableTrait;
import org.example.learn.java.spec.commons.model.anno.Scar;
import org.junit.Test;

public class InheritAnnotationTest {

    /**
     * 展示注解的继承机制
     *
     */
    @Test
    public void demoInheritanceMechanism() {
        // 被@Inherited注释的注解才会有继承特性
        InheritableTrait inheritableTraitOnSon = Son.class.getAnnotation(InheritableTrait.class);
        System.out.println("inheritableTraitOnSon = " + inheritableTraitOnSon);
        // 没有被@Inherited注释的注解不会有继承特性
        NonInheritableTrait nonInheritableTraitOnSon = Son.class.getAnnotation(NonInheritableTrait.class);
        System.out.println("nonInheritableTraitOnSon = " + nonInheritableTraitOnSon);

        // 注解A被@Inherited注释,注解A再去注释注解B,则注解B将不存在继承特征
        Scar scarOnSon = Son.class.getAnnotation(Scar.class);
        System.out.println("scarOnSon = " + scarOnSon);

        // 如果父子类都有相同的注解A, 那么java.lang.Class.getAnnotation方法返回的将是子类的注解;
        Handsome handsomeOnSon = Son.class.getAnnotation(Handsome.class);
        System.out.println("handsomeOnSon = " + handsomeOnSon);
        Handsome handsomeOnFather = Father.class.getAnnotation(Handsome.class);
        System.out.println("handsomeOnFather = " + handsomeOnFather);
    }

}
