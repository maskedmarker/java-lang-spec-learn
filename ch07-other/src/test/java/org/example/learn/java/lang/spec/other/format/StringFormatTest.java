package org.example.learn.java.lang.spec.other.format;

import org.junit.Test;

import java.util.Calendar;
import java.util.Formatter;

public class StringFormatTest {


    @Test
    public void test0() {
        Formatter formatter = new Formatter();
        Formatter log = formatter.format("thread[%s] is logging at [%s]", Thread.currentThread().getName(), Calendar.getInstance().getTime());
        // toString就是格式化后的字符串
        System.out.println("log = " + log);
    }

    /**
     * Formatter支持一步一步追加占位字符串
     */
    @Test
    public void test1() {
        Formatter formatter = new Formatter();
        // return This formatter
        Formatter log = formatter.format("thread[%s] is logging at [%s]", Thread.currentThread().getName(), Calendar.getInstance().getTime());
        System.out.println("log = " + log);
        // 还可以追加新增字符串(或带占位的字符串)
        log.format("|||新增内容%s||", "helloooo");
        // 返回最终的全部格式化后的字符串
        System.out.println("log = " + log);
    }
}
