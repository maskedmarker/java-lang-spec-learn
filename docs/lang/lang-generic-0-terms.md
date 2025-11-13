# java泛型术语


Java generics introduce several important terms that describe how types are declared, used, and resolved. 
Below is a structured explanation of the most common generic-related terminology.


## Type Parameter（类型参数）

A type parameter is the symbol (usually a single capital letter like T, E, K, or V) used in a generic declaration to represent a type that will be specified later.

```text
class Box<T> {    // "T" is a type parameter
    T value;
}

T stands for an unknown type.
When you define a class, interface, or method with <T>, you’re declaring a type parameter.
```


## Type Argument（类型实参）

A type argument is the actual type you supply to a generic type parameter when you instantiate or use the generic.

```text
Box<String> b = new Box<>();  // "String" is the type argument

T → type parameter
String → type argument

Analogy:
Method parameters → arguments
Type parameters → type arguments
```


## Generic Type（泛型类型）

A generic type is a class or interface that declares one or more type parameters.

```text
class Pair<K, V> {    // Generic type with two type parameters
    K key;
    V value;
}

Pair is a generic type.
Pair<String, Integer> is a parameterized type (see below).
```

## Parameterized Type（参数化类型）

A parameterized type is **a concrete instantiation of a generic type**, where all type parameters have been replaced by actual type arguments.

```text
List<String> names = new ArrayList<>();
Map<String, Integer> scores = new HashMap<>();

List<String> and Map<String, Integer> are parameterized types.
The generic types are List<E> and Map<K, V>.

In reflection, ParameterizedType represents this kind of type at runtime.
```


## Raw Type（原始类型）

A raw type is a generic type used without specifying its type arguments.
This feature exists for backward compatibility with pre-Java 5 code.

```text
List list = new ArrayList(); // Raw type
list.add("hello");
list.add(123);

// List (without <E>) is a raw type.
// Raw types disable compile-time type checking, so they’re not recommended.
```

注意: "A raw type is a generic type used without specifying its type arguments." 这句话应该准确讲是 "A raw type is a parameterized type used without specifying its type arguments."



## Type Variable（类型变量）

A type variable is an instance of TypeVariable<D> in reflection, representing a declared type parameter.

```text
class Box<T> {
    T value;
}

T is a type variable in the source code.At runtime, reflection sees it as a TypeVariable object.
在反射语境下,将Type Parameter讲做type variable
```



## Wildcard Type（通配符类型）

A wildcard allows partial specification of a type argument using the ? symbol.

```text
List<?> list;               // Unbounded wildcard
List<? extends Number> a;   // Upper-bounded wildcard
List<? super Integer> b;    // Lower-bounded wildcard


In reflection, a wildcard type is represented by the WildcardType interface.
Syntax	        Chinese	          Meaning
?	           无界通配符	          Any type
? extends T	   上界通配符	          Any subtype of T
? super T	   下界通配符	          Any supertype of T
```


## Generic Method（泛型方法）

A generic method declares its own type parameter(s), independent of any class-level parameters.


## Bounded Type Parameter（有限定类型参数）

A bounded type parameter restricts what types can be used as type arguments.

```text
class Box<T extends Number> { }     // Upper bound
class Container<T extends Comparable<T>> { }


T extends Number means T must be Number or a subclass of Number.
Multiple bounds: <T extends Number & Serializable>.
```

## Generic Signature（泛型签名）

The generic signature is the internal representation of a type’s generic structure in the class file (used by the JVM and reflection).

For example, List<String> might have a signature like:
```text
Ljava/util/List<Ljava/lang/String;>;
```

## Type Erasure（类型擦除）

Java uses type erasure to maintain backward compatibility — generic information is removed at compile time.

```text
List<String> list = new ArrayList<>();
List<Integer> ints = new ArrayList<>();
System.out.println(list.getClass() == ints.getClass()); // true


At runtime, both become simply List.
Type safety is enforced at compile time only.
```



## Reifiable vs Non-Reifiable Types（可具体化类型与不可具体化类型）

Reifiable Type（可具体化类型）: Type information fully available at runtime.
Examples: String, List<?>, int[]

Non-Reifiable Type（不可具体化类型）: Type information erased by the compiler.
Examples: List<String>, Map<String, Integer>

```text
Because of type erasure, you cannot do:

if (obj instanceof List<String>) { } // compile-time error
```

```text
Summary Table

| English Term           | Chinese Translation   | Example                       |
| ---------------------- | -------------------    | --------------------------------------------------------- |
| Generic Type           | 泛型类型                | List<E>, Map<K,V>                                         |
| Parameterized Type     | 参数化类型              | List<String>                                              |
| Raw Type               | 原始类型                | List                                                      |
| Type Parameter         | 类型参数                | <T> in class Box<T>                                       |
| Type Argument          | 类型实参                | String in Box<String>                                     |
| Wildcard Type          | 通配符类型              | List<? extends Number>                                    |
| Bounded Type Parameter | 有限定类型参数           | <T extends Number>                                        |
| Generic Method         | 泛型方法                | <T> void method(T t)                                      |
| Type Erasure           | 类型擦除                | Compile-time generics removal                             |
| Type Variable          | 类型变量                | T (reflection term 注意这是反射的术语)                       |

```