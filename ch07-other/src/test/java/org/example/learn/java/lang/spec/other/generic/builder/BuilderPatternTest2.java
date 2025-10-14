package org.example.learn.java.lang.spec.other.generic.builder;

import org.junit.Test;

/**
 * builder模式
 * 加入了泛型/抽象类
 */
public class BuilderPatternTest2 {

    public static abstract class BaseBuilder<T, B extends BaseBuilder<T, B>> {

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        protected abstract T build();
    }

    public static class MyObjectBuilder extends BaseBuilder<MyObject, MyObjectBuilder> {

        private String field1;

        private String field2;

        public MyObjectBuilder setField1(String field1) {
            this.field1 = field1;
            return self();
        }

        public MyObjectBuilder setField2(String field2) {
            this.field2 = field2;
            return self();
        }

        @Override
        public MyObject build() {
            return new MyObject(this);
        }
    }

    public static class MyObject {

        private String field1;

        private String field2;

        // private是为了不允许其他类new实例,只能通过Builder创建实例
        private MyObject(MyObjectBuilder builder) {
            this.field1 = builder.field1;
            this.field2 = builder.field2;
        }

        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }
    }


    @Test
    public void test0() {
        MyObject myObject = new MyObjectBuilder()
                .setField1("hello")
                .setField2("world")
                .build();

        System.out.println("myObject.getField1() = " + myObject.getField1());
        System.out.println("myObject.getField2() = " + myObject.getField2());
    }
}
