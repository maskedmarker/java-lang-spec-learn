package org.example.learn.java.lang.spec.other.generic.builder;

import org.junit.Test;

/**
 * 最简单的builder模式
 */
public class BuilderPatternTest1 {

    public static class MyObject {

        private String field1;

        private String field2;

        // private是为了不允许其他类new实例,只能通过Builder创建实例
        private MyObject(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }

        public static MyObjectBuilder newBuilder() {
            return new MyObjectBuilder();
        }


        public static class MyObjectBuilder {

            private String field1;

            private String field2;

            private MyObjectBuilder() {
            }

            public MyObjectBuilder setField1(String field1) {
                this.field1 = field1;
                return this;
            }

            public MyObjectBuilder setField2(String field2) {
                this.field2 = field2;
                return this;
            }

            public MyObject build() {
                return new MyObject(this.field1, this.field2);
            }
        }
    }


    @Test
    public void test0() {
        MyObject myObject = MyObject.newBuilder()
                .setField1("hello")
                .setField2("world")
                .build();

        System.out.println("myObject.getField1() = " + myObject.getField1());
        System.out.println("myObject.getField2() = " + myObject.getField2());
    }
}
