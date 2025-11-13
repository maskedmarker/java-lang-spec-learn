# java递归泛型(self-referential generics)

递归泛型,就是一个泛型类或接口,其类型参数被定义为其自身.
这听起来有点“我生了我自己”的悖论感,但它在编程中非常有用,主要目的是为了在继承链中保持类型的精确性.

The Canonical Form
```text
class SelfBounded<T extends SelfBounded<T>> {
    // ...
}
```

## 为什么需要它？

为什么需要它？—— 一个经典问题
我们通过一个例子来看.假设我们想构建一个“动物”继承体系,并且每种动物都有一个compareTo方法,用来和同种类的动物比较.

第一版尝试（没有递归泛型）：
```text
class Animal {
    protected String name;
    protected int weight;

    public int compareTo(Animal other) {
        return Integer.compare(this.weight, other.weight);
    }
}

class Dog extends Animal {
    // 狗狗特有的属性和方法
    public void fetch() {
        System.out.println("Fetching the ball!");
    }
}

现在问题来了：

Dog myDog = new Dog();
Dog yourDog = new Dog();
Animal someAnimal = new Animal();

myDog.compareTo(yourDog); // 没问题,两个Dog比较
myDog.compareTo(someAnimal); // 语法上没问题,但逻辑上有问题！
```

第二版尝试（在Dog里重写compareTo）：
```text
class Dog extends Animal {
    @Override
    public int compareTo(Animal other) {
        // 为了安全,我们得先检查类型
        if (!(other instanceof Dog)) {
            throw new ClassCastException("Cannot compare Dog with " + other.getClass().getSimpleName());
        }
        Dog otherDog = (Dog) other;
        return Integer.compare(this.weight, otherDog.weight);
    }
}

这样虽然运行时安全了,但有两个大缺点：
编译时无法发现问题：myDog.compareTo(someAnimal)在编译时依然通过,只有运行时才会崩溃.
代码冗余：每个子类（如Cat, Bird）都需要重写compareTo并做同样的类型检查和强制转换.
```

解决方案：引入递归泛型
```text
1. 定义带有递归泛型的基类

// T 是某种具体的动物类型,它自己也是 Animal<T> 的子类型
class Animal<T extends Animal<T>> implements Comparable<T> {
    protected String name;
    protected int weight;

    @Override
    public int compareTo(T other) {
        // 现在,other 的类型就是 T,而不是笼统的 Animal
        // 这意味着它一定是当前类的具体子类型（比如Dog）
        return Integer.compare(this.weight, other.weight);
    }
}

我们来拆解一下 Animal<T extends Animal<T>>：
T 是一个类型参数.
T 必须 扩展 Animal<T>.
这形成了一个递归：Animal 的类型依赖于 T,而 T 又依赖于 Animal<T>.


2. 定义子类

// Dog 继承自 Animal<Dog>
class Dog extends Animal<Dog> {
    public void fetch() {
        System.out.println("Fetching the ball!");
    }
}

// Cat 继承自 Animal<Cat>
class Cat extends Animal<Cat> {
    public void meow() {
        System.out.println("Meow!");
    }
}

注意看,
Dog 填入了 Dog 作为 T 的具体类型.所以对于 Dog 类来说,它继承的是 Animal<Dog>.
Cat 填入了 Cat 作为 T 的具体类型.所以对于 Cat 类来说,它继承的是 Animal<Cat>.


3. 效果

现在,我们再来看比较方法：
Dog myDog = new Dog();
Dog yourDog = new Dog();
Cat myCat = new Cat();

myDog.compareTo(yourDog); // ✅ 完美！编译通过,类型安全
myDog.compareTo(myCat);   // ❌ 编译错误！编译器直接报错


为什么 myDog.compareTo(myCat) 会编译错误？

myDog 的类型是 Dog.
Dog 类继承自 Animal<Dog>.
Animal<Dog> 实现了 Comparable<Dog> 接口.
因此,Dog 的 compareTo 方法只接受 Dog 类型的参数.
Cat 不是 Dog,所以编译器直接拒绝.



核心理解与总结
目的：
递归泛型的核心目的是在继承关系中“锁定”具体的类型,确保在父类中定义的方法（如compareTo）其参数类型与最终子类的类型一致,从而实现编译时的类型安全.

现实世界的例子：
Java 的 Enum：Enum<E extends Enum<E>>.这确保了 Color.RED.compareTo(Color.BLUE) 是合法的,而 Color.RED.compareTo(Thread.State.NEW) 是非法的.
Builder 模式：在构建器模式中,递归泛型常用于让父类的构建器方法返回子类的类型,从而实现流畅的接口调用.

理解技巧：
不要纠结于“T 到底是谁”的无限递归,把它看作一种契约或承诺：
基类 Animal 说：“我的子类 T 必须承诺,它是我 Animal 的一种具体类型.”
子类 Dog 在继承时说：“我承诺,我就是那个具体的类型 Dog.”
这样一来,在 Animal 的代码里,所有用到 T 的地方,都被替换成了最终那个信守承诺的子类（如 Dog）.
```
思考: 这种递归泛型本质解决了,类型参数无法指向当前类的逻辑漏洞,至于递归泛型这个代码格式是如何编写的,就由Java语言委员会来定义.


递归泛型这种结构允许父类方法返回子类类型(因为类型参数化).
递归泛型常用于Builder模式,你在 Netty、Guava、Lombok 等源码中会频繁看到.

```text
// 抽象父类,使用递归泛型 B
public abstract class AbstractBuilder<B extends AbstractBuilder<B>> {

    private String name;

    // 返回 B 类型,以支持链式调用
    public B setName(String name) {
        this.name = name;
        return self();  // 关键点
    }

    protected abstract B self(){
        return (B) this; // 返回实际子类实例
    } 

    public void build() {
        System.out.println("Build with name = " + name);
    }
}


子类实现
public class MyBuilder extends AbstractBuilder<MyBuilder> {

    private int age;

    public MyBuilder setAge(int age) {
        this.age = age;
        return self();
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