# lambda字节码指令实现

invokedynamic指令实现了dynamic-linkage


## 语法

```text
invokedynamic (a symbolic reference to a call site specifier)
```

a symbolic reference指定就是字节码文件BootstrapMethods部分的索引编号

call site specifier包括3部分
```text
A symbolic reference to a call site specifier is derived from a CONSTANT_InvokeDynamic_info structure (§4.4.10) in the binary representation of a class or interface. 
Such a reference gives:
– a symbolic reference to a method handle, which will serve as a bootstrap method for an invokedynamic instruction;
– a sequence of symbolic references (to classes, method types, and method handles), string literals, and run-time constant values which will serve as static arguments to a bootstrap method;
– a method name and method descriptor.
```


## 详解

The `invokedynamic` instruction in the Java Virtual Machine (JVM) is a powerful mechanism introduced in **Java 7** 
to support **dynamic languages** and optimize the runtime behavior of certain constructs like lambda expressions and method references.

### Purpose of `invokedynamic`:
The main goal of the `invokedynamic` instruction is to **defer method linkage** until runtime, allowing for **more flexible and dynamic method dispatch** 
compared to traditional Java method calls (which are statically linked). 
This flexibility is essential for supporting dynamic features such as those found in languages like Python, Ruby, and JavaScript, 
which are dynamically typed and have dynamic method dispatch.

For Java, `invokedynamic` is particularly useful in the context of **lambda expressions** and **method references**, 
which were introduced in Java 8 as part of functional programming support.

### How `invokedynamic` Works:

1. **Dynamic Call Site**:
   A **call site** is where a method is invoked, and this call site can be resolved dynamically at runtime using `invokedynamic`. Instead of statically linking the method at compile-time, `invokedynamic` defers the linkage to a **dynamic method** that can be resolved during execution.

2. **Bootstrap Method**:
   To facilitate dynamic linking, `invokedynamic` uses a **bootstrap method** (a special method defined in the bytecode). The bootstrap method is responsible for resolving the actual method to be invoked at runtime.

   When the JVM encounters an `invokedynamic` instruction, it calls the bootstrap method to figure out how to link the method (i.e., which method to invoke, whether it's a lambda or method reference).

3. **Dynamic Method Handle**:
   The bootstrap method returns a **method handle** (an instance of `java.lang.invoke.MethodHandle`), which represents a specific method (or constructor) to be invoked. Once the JVM has the method handle, it can perform the invocation just like any normal method call.

4. **Optimized Invocation**:
   Once the method handle is resolved and linked, the JVM can optimize it for future calls, just like a regular method call. This makes subsequent calls to the same method more efficient.

### Example in the Context of Lambdas:

When you write a lambda expression, the Java compiler translates the expression into an `invokedynamic` instruction in the bytecode. The instruction links to a **bootstrap method** that will resolve the lambda to a concrete implementation of the corresponding functional interface.

**Example of how `invokedynamic` is used with lambdas**:
```text
Runnable r = () -> System.out.println("Hello, World!");
r.run();
```

Here’s what happens behind the scenes:
1. The lambda expression `() -> System.out.println("Hello, World!")` gets translated into a `LambdaMetafactory` call.
2. The `LambdaMetafactory` uses `invokedynamic` to create a call site that points to a dynamically resolved method that implements the `Runnable` interface’s `run` method.
3. The JVM invokes the **bootstrap method** to resolve the method handle for the lambda.
4. Once resolved, the method handle points to the correct implementation, and the `run` method is executed.

### How it Improves Java:
- **Dynamic Resolution**: `invokedynamic` allows for dynamic language features in the JVM, making it easier to implement things like lambdas and other dynamic behaviors that are harder to express using traditional static method calls.
- **Performance**: The JVM can optimize `invokedynamic` call sites at runtime, which can make repeated calls faster after the initial lookup. This is particularly helpful for lambdas, where the method to invoke is not known at compile-time.
- **Support for Non-Java Languages**: `invokedynamic` is designed to make it easier for dynamic languages to run on the JVM by providing a flexible and performant way to resolve method calls at runtime.

### Summary:

- **`invokedynamic`** allows for **dynamic method invocation** at runtime.
- It uses a **bootstrap method** to dynamically resolve a method to be invoked at the call site.
- It is heavily used in **lambda expressions** and **method references** in Java, providing flexibility and runtime efficiency.
- By allowing dynamic dispatch, it optimizes performance over time, making it ideal for features like functional programming constructs in Java.