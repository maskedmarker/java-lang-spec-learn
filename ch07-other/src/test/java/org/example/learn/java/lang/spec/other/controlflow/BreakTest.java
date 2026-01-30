package org.example.learn.java.lang.spec.other.controlflow;

import org.junit.Assert;
import org.junit.Test;

/**
 * Java 支持“标签”跳出多层嵌套循环
 */
public class BreakTest {

    @Test
    public void test01() {
        outer: for (int i = 0; i < 5; i++) {
            System.out.println("begin i = " + i);
            for (int j = 0; j < 5; j++) {
                System.out.println("j = " + j);
                if (j == 3) break outer;
            }
            System.out.println("end i = " + i);
        }
    }

    @Test
    public void test02() {
        outer: for (int i = 0; i < 5; i++) {
            System.out.println("begin i = " + i);
            for (int j = 0; j < 5; j++) {
                System.out.println("j = " + j);
                if (j == 3) continue outer;
            }
            System.out.println("end i = " + i);
        }
    }

    @Test
    public void test11() {
        int r = 0;
        tryOne: if(r < 5) {
            System.out.println("r = " + (r++));
        }
        Assert.assertEquals(1, r);

        r = 0;
        tryOne: if(r < 5) {
            System.out.println("r = " + (r++));
            System.out.println("break+tag-if,可以提前结束if语句块");
            if (r == 1) break tryOne;
            System.out.println("这条语句不可能被执行");
        }
        Assert.assertEquals(1, r);


        System.out.println("------------------------------------------------");

        int x = 0;
        tryTwo: while(x < 5) {
            System.out.println("x = " + (x++));
            if (x == 3) break tryTwo;
        }
        Assert.assertEquals("tag本身并不会导致循环.tag仅仅用来跳出指定的语句块", 3, x);
    }
}