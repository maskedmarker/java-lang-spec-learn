# java lambda expression

A lambda expression is a concise way to represent an instance of a functional interface (an interface with a single abstract method).
The syntax for a lambda expression is:
```text
(parameter1, parameter2, ...) -> expression or statement block
```

Lambda expressions are internally compiled into invokedynamic bytecode using the LambdaMetafactory. 
The method or function that receives the lambda expression calls this dynamically linked method at runtime.

Lambda expressions are implemented using invokedynamic and LambdaMetafactory to generate functionally equivalent anonymous classes.
Method references are syntactic sugar over lambdas, referring directly to methods and are linked similarly at runtime.


## 实现原理
具体实现请看 bytecode-op-invokedynamic.md




