package org.example.learn.java.lang.spec.other.operator;

import org.junit.Test;

/**
 * 逻辑运算符(logical operator)
 * && || !
 *
 * 比较运算符构成的比较表达式的值是逻辑true或false
 */
public class LogicalOperatorTest {

    /**
     * 注意: 下面的逻辑表达式优先级
     */
    @Test
    public void test10() {
        // 在逻辑表达式中, 逻辑与的优先级高于逻辑否
        if (!true && false) { // 等价于 !(true & false)
            throw new RuntimeException();
        } else {
            System.out.println("逻辑与的优先级高于逻辑否");
        }
    }
}
