# java反射



```text
java.lang.reflect.Type

In Java, java.lang.reflect.Type is an interface (not a class) that serves as the common superinterface for all types in the Java reflection system. 
It represents any kind of Java type that can appear in code, including both raw and parameterized types, array types, type variables and primitive types.
Essentially, Type provides a unified abstraction over different kinds of type information at runtime.


Purpose
The purpose of Type is to enable Java reflection APIs to describe generic type information (introduced in Java 5) — something that older reflection APIs couldn’t represent.
Before generics, Class<?> was enough to describe any type, but after generics were added, more complex types (like List<String>, Map<K, V>, arrays, wildcards, and type variables) needed a richer system.


Subinterfaces and Implementations

| Type Implementation | Represents Example    | Description                                                           |
| ------------------- | --------------------- | --------------------------------------------------------------------- |
| Class<T>            | String, Integer[]     | A concrete class or interface.                                        |
| ParameterizedType   | List<String>          | A generic type with parameters.                                       |
| TypeVariable<D>     | T in class Box<T>     | A type parameter variable.                                            |
| GenericArrayType    | T[]                   | An array whose element type is a parameterized type or type variable. |
| WildcardType        | ? extends Number      | A wildcard type used in generics.                                     |

```


```text
java.lang.reflect.ParameterizedType

ParameterizedType represents a parameterized type such as Collection<String>.
A parameterized type is created the first time it is needed by a reflective method, as specified in this package. 
When a parameterized type p is created, the generic type declaration that p instantiates is resolved, and all type arguments of p are created recursively.
```

```text
java.lang.reflect.TypeVariable

TypeVariable is the common superinterface for type variables of kinds. 
A type variable is created the first time it is needed by a reflective method, as specified in this package. 
If a type variable t is referenced by a type (i.e, class, interface or annotation type) T, and T is declared by the nth enclosing class of T (see JLS 8.1.2), then the creation of t requires the resolution (see JVMS 5) of the ith enclosing class of T, for i = 0 to n, inclusive. Creating a type variable must not cause the creation of its bounds. Repeated creation of a type variable has no effect.

```