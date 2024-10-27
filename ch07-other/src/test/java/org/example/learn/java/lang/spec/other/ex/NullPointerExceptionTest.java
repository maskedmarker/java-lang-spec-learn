package org.example.learn.java.lang.spec.other.ex;

import org.junit.Test;

/**
 * NullPointerException的toString内容为 java.lang.NullPointerException: text
 * 所以new NullPointerException(text)中的text应该包含引起空指针的变量的变量名,这样方便排查是哪个变量引起的空指针异常.
 * text 可以仅仅是变量名nullVar; 也可以是更加友好的文字,比如nullVar should not be null.
 * <p>
 *
 * jdk中部分关于NullPointerException的使用:
 * <ul>
 * <li>throw new NullPointerException("The window argument should not be null.")
 * <li>throw new NullPointerException("The point argument must not be null")
 * <li>throw new NullPointerException("options contains 'null'")
 * <li>throw new NullPointerException("cannot bind to null")
 * <li>throw new NullPointerException("permission can't be null")
 * </ul>
 * @author cjh
 * @date 2022/10/22
 */
public class NullPointerExceptionTest {

    @Test
    public void test() {
        String nullVar = null;
        // Exception in thread "main" java.lang.NullPointerException: nullVar
        checkNotNull(nullVar, "nullVar");
    }

    public <T> T checkNotNull(T arg, String text) {
        if (arg == null) {
            throw new NullPointerException(text);
        }
        return arg;
    }
}
