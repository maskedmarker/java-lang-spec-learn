# bridge method

## 协变返回类型
协变返回类型是指子类方法的返回值类型不必严格等同于父类中被重写的方法的返回值类型,而可以是更 “具体” 的类型.
在Java 1.5添加了对协变返回类型的支持,即子类重写父类方法时,返回的类型可以是子类方法返回类型的子类.

下面看一个例子,Child类重写其父类Parent的get方法,Parent的get方法返回类型为Number,而Child类中get方法返回类型为Integer.
从上面的结果可以看到,有一个方法java.lang.Number get(), 在源码中是没有出现过的,是由编译器自动生成的,该方法被标记为ACC_BRIDGE和ACC_SYNTHETIC,就是我们前面所说的桥接方法.
这个方法就起了一个桥接的作用,它所做的就是把对自身的调用通过invokevirtual指令再调用方法java.lang.Integer get().
编译器这么做的原因是什么呢？
因为在JVM方法中,返回类型也是方法签名的一部分,而桥接方法的签名和其父类的方法签名一致,以此就实现了协变返回值类型.

```java
public class Parent {
    Number get() {
        return 1;
    }
}

public class Child extends Parent {

    @Override
    Integer get() {
        return 1;
    }
}
```

```opcode
public class Child extends Parent
......省略部分结果......
  java.lang.Integer get();
    descriptor: ()Ljava/lang/Integer;
    flags:
    Code:
      stack=1, locals=1, args_size=1
         0: iconst_1
         1: invokestatic  #2                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
         4: areturn
      LineNumberTable:
        line 5: 0

  java.lang.Number get();
    descriptor: ()Ljava/lang/Number;
    flags: ACC_BRIDGE, ACC_SYNTHETIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokevirtual #3                  // Method get:()Ljava/lang/Integer;
         4: areturn
      LineNumberTable:
        line 1: 0

```


## 类型擦除
泛型是Java 1.5才引进的概念,在这之前是没有泛型的概念的,但泛型代码能够很好地和之前版本的代码很好地兼容,这是为什么呢？
这是因为,在编译期间Java编译器会将类型参数替换为其上界（类型参数中extends子句的类型）,如果上界没有定义,则默认为Object,这就叫做类型擦除.
当一个子类在继承（或实现）一个父类（或接口）的泛型方法时,在子类中明确指定了泛型类型,那么在编译时编译器会自动生成桥接方法,例如：

```java
public class Parent<T> {

    void set(T t) {
    }
}

public class Child extends Parent<String> {

    @Override
    void set(String str) {
    }
}
```

```opcode
public class Child extends Parent<java.lang.String>
......省略部分结果......
  void set(java.lang.String);
    descriptor: (Ljava/lang/String;)V
    flags:
    Code:
      stack=0, locals=2, args_size=2
         0: return
      LineNumberTable:
        line 5: 0

  void set(java.lang.Object);
    descriptor: (Ljava/lang/Object;)V
    flags: ACC_BRIDGE, ACC_SYNTHETIC
    Code:
      stack=2, locals=2, args_size=2
         0: aload_0
         1: aload_1
         2: checkcast     #2                  // class java/lang/String
         5: invokevirtual #3                  // Method set:(Ljava/lang/String;)V
         8: return
      LineNumberTable:
        line 1: 0
```

从上面的结果可以看到,有一个方法void set(java.lang.Object), 在源码中是没有出现过的,是由编译器自动生成的,该方法被标记为ACC_BRIDGE和ACC_SYNTHETIC,就是我们前面所说的桥接方法.
这个方法就起了一个桥接的作用,它所做的就是把对自身的调用通过invokevirtual指令再调用方法void set(java.lang.String).

编译器这么做的原因是什么呢？
因为Parent类在类型擦除之后,变成这样：

```java
public class Parent<Object> {

    void set(Object t) {
    }
}
```

编译器为了让子类有一个与父类的方法签名一致的方法,就在子类自动生成一个与父类的方法签名一致的桥接方法.