package org.example.learn.java.spec.reflection.generic;

public class RecursiveTest1BuilderPattern {

    public class Builder<T extends Builder<T>> {

        private String field;

        public T setField(String field) {
            this.field = field;
            return self();
        }

        protected T self() {
            return (T) this;
        }

        public MyObject build() {
            return new MyObject(this);
        }
    }

    public class MyObject {
        private final String field;

        private MyObject(Builder<?> builder) {
            this.field = builder.field;
        }
    }
}
