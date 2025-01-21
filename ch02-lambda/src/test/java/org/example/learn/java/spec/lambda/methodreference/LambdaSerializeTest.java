package org.example.learn.java.spec.lambda.methodreference;

import org.example.learn.java.spec.lambda.serialize.SFunction;
import org.example.learn.java.spec.lambda.serialize.SerializedLambda;
import org.example.learn.java.spec.lambda.serialize.SerializedLambdaUtils;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * method reference对象序列化时,会包含metadata信息
 */
public class LambdaSerializeTest {

    private static class StringProcessor {
        public String process(String input) {
            return input.toUpperCase();
        }
    }

    @Test
    public void test1() {
        StringProcessor processor = new StringProcessor();
        Function<String, String> function = processor::process;
        System.out.println("function.getClass() = " + function.getClass());
    }

    /**
     * 必须实现Serializable接口,才能序列化
     */
    private static class StringProcessor2 implements Serializable {
        private static final long serialVersionUID = 1L;

        public String process(String input) {
            return input.toUpperCase();
        }
    }

    @Test
    public void test2() {
        StringProcessor2 processor = new StringProcessor2();
        SFunction<String, String> function = processor::process;
        System.out.println("function.getClass() = " + function.getClass());

        SerializedLambda serializedLambda = SerializedLambdaUtils.resolve(function);
        System.out.println("serializedLambda = " + serializedLambda);

        System.out.println("serializedLambda.getFunctionalInterfaceClassName() = " + serializedLambda.getFunctionalInterfaceClassName());
        System.out.println("serializedLambda.getImplMethodName() = " + serializedLambda.getImplMethodName());
        System.out.println("serializedLambda.getInstantiatedType() = " + serializedLambda.getInstantiatedType());
        System.out.println("serializedLambda.getImplClass() = " + serializedLambda.getImplClass());
    }

    private static class StringProcessor3 implements Serializable {
        private static final long serialVersionUID = 1L;

        public List<String> process(String input) {
            List<String> result = new ArrayList<>();
            result.add(input.toUpperCase());
            return result;
        }
    }

    @Test
    public void test3() {
        StringProcessor3 processor = new StringProcessor3();
        SFunction<String, List<String>> function = processor::process;
        System.out.println("function.getClass() = " + function.getClass());

        SerializedLambda serializedLambda = SerializedLambdaUtils.resolve(function);
        System.out.println("serializedLambda = " + serializedLambda);

        System.out.println("serializedLambda.getFunctionalInterfaceClassName() = " + serializedLambda.getFunctionalInterfaceClassName());
        System.out.println("serializedLambda.getImplMethodName() = " + serializedLambda.getImplMethodName());
        // 类型实例参数
        System.out.println("serializedLambda.getInstantiatedType() = " + serializedLambda.getInstantiatedType());
        System.out.println("serializedLambda.getImplClass() = " + serializedLambda.getImplClass());
    }
}
