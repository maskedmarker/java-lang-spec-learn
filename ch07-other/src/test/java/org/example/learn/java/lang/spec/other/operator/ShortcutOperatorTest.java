package org.example.learn.java.lang.spec.other.operator;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ShortcutOperatorTest {


    @Test
    public void test01() {
        int top = 0;
        int r = top++; // 这个语句等价于 r=top;top++;
        System.out.println("s = " + r);
        Assert.assertEquals("r=top++ 中, 先将top的原值赋值到变量r,然后top再自增", top - 1, r);
    }


}
