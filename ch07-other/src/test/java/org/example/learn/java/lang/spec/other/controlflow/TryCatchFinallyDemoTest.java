package org.example.learn.java.lang.spec.other.controlflow;

import org.junit.Before;
import org.junit.Test;

public class TryCatchFinallyDemoTest {

    @Test
    public void testNoException() {
        String result = this.execOrder(1);
        System.out.println("result = " + result);
    }

    @Test
    public void testException() {
        String result = this.execOrder(0);
        System.out.println("result = " + result);
    }

    /**
     * 测试控制流
     * @param i 当i为0时,发生异常
     * @return 执行结果
     */
    public String execOrder(int i) {
        String result;
        try {
            int r = 1 / i;
            result = "set by try";
            return result;
        } catch (Exception e) {
            result = "set by exception";
            return result;
        } finally {
            result = "set by finally";
            return result;
        }
    }
}