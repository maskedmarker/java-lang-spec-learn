package org.example.learn.java.lang.spec.math.bool;

import org.junit.Assert;
import org.junit.Test;

/**
 * bool运算符
 * & | ^ 在不同类型的操作数下,表达的意思是不同的
 * 操作数为bool类型时, 逻辑与运算符(&)/逻辑或运算符(|)/逻辑异或运算符(^)
 * 操作数为整数类型(byte/short/int/long)时, 位与运算符(&)/位或运算符(|)/位异或运算符(^)
 */
public class BoolOperatorTest {

    /**
     * 逻辑运算符
     */
    @Test
    public void test0() {
        // 逻辑与运算符(&)
        Assert.assertEquals("逻辑与运算符,operand必须都是true,结果才是true", true, true & true);
        Assert.assertEquals("逻辑与运算符,operand有一个是false,结果就是false", false, true & false);
        Assert.assertEquals("逻辑与运算符,operand有一个是false,结果就是false", false, false & true);

        // 逻辑或运算符(|)
        Assert.assertEquals("逻辑或运算符,operand有一个是true,结果就是true", true, true | false);
        Assert.assertEquals("逻辑或运算符,operand有一个是true,结果就是true", true, false | true);
        Assert.assertEquals("逻辑或运算符,operand都是false,结果才是false", false, false | false);

        // 逻辑异或运算符(^)
        Assert.assertEquals("逻辑异或运算符,operand不相同时,结果就是true", true, true | false);
        Assert.assertEquals("逻辑异或运算符,operand不相同时,结果就是true", true, false | true);
        Assert.assertEquals("逻辑异或运算符,operand相同时,结果就是false", false, false ^ false);
        Assert.assertEquals("逻辑异或运算符,operand相同时,结果就是false", false, true ^ true);
    }

    /**
     * 为了较少迷惑, 下面是相同符号的位运算符
     */
    @Test
    public void test1() {
        // 位与运算符(&)
        Assert.assertEquals("位与运算符,operand的每个bit取与运算", 0b0000, (0b0001 & 0b0010));


        // 位或运算符(|)
        Assert.assertEquals("位或运算符,operand的每个bit取或运算", 0b0011, (0b0001 | 0b0010));
        Assert.assertEquals("位或运算符,operand的每个bit取或运算", 0b1011, (0b1001 | 0b1010));


        // 位异或运算符(^)
        Assert.assertEquals("位或运算符,operand的每个bit取异或运算", 0b0011, (0b0001 ^ 0b0010));
        Assert.assertEquals("位或运算符,operand的每个bit取异或运算", 0b0011, (0b1001 ^ 0b1010));
    }
}
