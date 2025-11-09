package org.example.learn.java.lang.spec.other.operator;

import org.junit.Test;

/**
 * 逻辑运算符
 */
public class LogicOperatorTest {


    /**
     * 优先级
     */
    @Test
    public void test0() {
        // 在逻辑表达式中, 逻辑与的优先级高于逻辑否
        if (!true & false) { // 等价于 !(true & false)
            throw new RuntimeException();
        } else {
            System.out.println("逻辑与的优先级高于逻辑否");
        }
    }
}
