# java泛型



## 递归泛型

除了常规使用泛型的场景,还有一种比较难理解的泛型使用场景-即递归泛型.
递归泛型这种结构允许父类方法返回子类类型.
递归泛型常用于Builder模式,你在 Netty、Guava、Lombok 等源码中会频繁看到.


```text
// 抽象父类，使用递归泛型 B
public abstract class AbstractBuilder<B extends AbstractBuilder<B>> {

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


子类实现
public class MyBuilder extends AbstractBuilder<MyBuilder> {

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

```


### 递归泛型解释

Recursive generics (also called self-referential generics or F-bounded polymorphism) refer to a situation where a generic type parameter is bounded by the type that declares it — that is, 
a type refers to itself as part of its own generic constraint.

In simpler terms, the generic type says:
“My type parameter must be a subclass (or implementation) of me.”

目的:
父类方法可以返回子类型(因为子类被定义成了类型参数type parameter),这样子类在使用父类方法时就不用cast强转,同时也保证了type precision across inheritance.

```text
1. The Canonical Form

class SelfBounded<T extends SelfBounded<T>> {
    // ...
}

Here, the type parameter T is bounded by SelfBounded<T>
so T must itself be a subclass of SelfBounded<T>.

This pattern is called recursive because the definition of the bound refers back to the type itself.
```

```text
This pattern enables type-safe inheritance and fluent APIs.
When a class extends a generic type that refers to itself, the generic parameter helps ensure that methods in the base class return the subclass type instead of the base type.


Without recursive generics

class Base {
    Base self() {
        return this;
    }
}

class Sub extends Base {
    Sub foo() {
        // The 'self()' method returns Base, not Sub
        return (Sub) self();  // needs cast
    }
}

Here, you lose type information — self() returns Base, even in Sub.



With recursive generics

class Base<T extends Base<T>> {
    T self() {
        return (T) this;
    }
}

class Sub extends Base<Sub> {
    Sub foo() {
        return self(); // no cast needed
    }
}

Now the compiler knows:
    In Sub, T is Sub.
    So self() returns Sub.
This eliminates the need for unsafe casts and maintains type precision across inheritance.
```

#### Common Real-World Examples
```text
java.lang.Enum

public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable {
    // ...
}

Every enum type in Java implicitly extends java.lang.Enum


abstract class Builder<T extends Builder<T>> {
    T setName(String name) {
        System.out.println("Set name: " + name);
        return (T) this;
    }
}


```