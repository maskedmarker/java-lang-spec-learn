package org.example.learn.java.lang.spec.other.operator;

import jdk.nashorn.internal.runtime.regexp.JoniRegExp;
import org.junit.Assert;
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
     *
     * 逻辑否的优先级高于逻辑与
     */
    @Test
    public void test01() {
        // 在逻辑表达式中, 逻辑否的优先级高于逻辑与
        if (! true && false) { // 等价于 (!true) && false
            throw new RuntimeException("不应该发生");
        } else {
            System.out.println("逻辑否的优先级高于逻辑与");
        }
    }

    /**
     * 逻辑与的优先级高于逻辑或
     */
    @Test
    public void test02() {
        // 在逻辑表达式中, 逻辑否的优先级高于逻辑与
        if (false && false || true) { // 等价于 (false && false) || true
            System.out.println("逻辑与的优先级高于逻辑或");
        } else {
            throw new RuntimeException("不应该发生");
        }
    }


    /**
     * 逻辑与的优先级高于逻辑否
     */
    @Test
    public void test03() {
        boolean a = (! isRunning() && removeJob());
        boolean b = (! removeJob() && isRunning());

        Assert.assertNotEquals("只有当&&的优先级大于!时,交换isRunning()和removeJob()并不会改变结果", a, b);
    }

    private boolean isRunning() {
        return true;
    }

    private boolean removeJob() {
        return false;
    }
}
