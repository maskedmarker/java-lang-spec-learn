package org.example.learn.java.lang.spec.other.generic;

import org.junit.Test;

public class RecursiveGenericTest {

    private abstract class AbstractBuilder<B extends AbstractBuilder<B>> {

        private String name;

        // 返回 B 类型，以支持链式调用
        public B setName(String name) {
            this.name = name;
            return self();  // 关键点
        }

        protected abstract B self(); // 返回实际子类实例

        public void build() {
            System.out.println("Build with name = " + name);
        }
    }

    private class MyBuilder extends AbstractBuilder<MyBuilder> {

        private int age;

        public MyBuilder setAge(int age) {
            this.age = age;
            return this;
        }

        @Override
        protected MyBuilder self() {
            return this;
        }

        @Override
        public void build() {
            super.build();
            System.out.println("Build with age = " + age);
        }
    }


    @Test
    public void test0() {
        new MyBuilder()
                .setName("Alice")     // 来自父类，返回的是 MyBuilder 类型
                .setAge(30)           // 来自子类
                .build();             // 输出结果
    }
}
