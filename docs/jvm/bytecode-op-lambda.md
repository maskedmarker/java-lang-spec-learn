# 字节码指令

## lambda bytecode
```java
package org.example.learn.java.spec.lambda.bytecode;

import org.junit.Test;

public class LambdaBytecodeTest {

    private static class MyClass {
        static void printMessage() {
            System.out.println("Hello, World!");
        }
    }

    @Test
    public void test0() {
        Runnable r = MyClass::printMessage;  // Method reference
        r.run();
    }
}
```


编译后的字节码文件中,重点关注Constant pool中的#2/#4
InvokeDynamic指令的第一个参数指的是BootstrapMethods部分的call-site索引编号;
InvokeDynamic指令的第二个参数指的是lambda expression所要实现的functional interface;

call-site描述了动态调用的实现信息.
在当前jdk的实现中,同时静态方法LambdaMetafactory.metafactory来动态实现lambda expression.
再具体的信息需要后续进一步学习.

```text
Constant pool:
   #2 = InvokeDynamic      #0:#34         // #0:run:()Ljava/lang/Runnable;
   #4 = InvokeDynamic      #1:#34         // #1:run:()Ljava/lang/Runnable;

   
BootstrapMethods:
  0: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
    Method arguments:
      #32 ()V
      #33 invokestatic org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest$MyClass.printMessage:()V
      #32 ()V
  1: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
    Method arguments:
      #32 ()V
      #37 invokestatic org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest.lambda$test1$0:()V
      #32 ()V
```

编译后的字节码文件内容如下
```text
Classfile /E:/git-repo/cjh-repo/java-lang-spec-learn/ch02-lambda/target/test-classes/org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest.class
  Last modified 2025-1-25; size 1528 bytes
  MD5 checksum b576e10399b6b9be90e8467ee8a8293c
  Compiled from "LambdaBytecodeTest.java"
public class org.example.learn.java.spec.lambda.bytecode.LambdaBytecodeTest
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #9.#29         // java/lang/Object."<init>":()V
   #2 = InvokeDynamic      #0:#34         // #0:run:()Ljava/lang/Runnable;
   #3 = InterfaceMethodref #35.#36        // java/lang/Runnable.run:()V
   #4 = InvokeDynamic      #1:#34         // #1:run:()Ljava/lang/Runnable;
   #5 = Fieldref           #38.#39        // java/lang/System.out:Ljava/io/PrintStream;
   #6 = String             #40            // hello world
   #7 = Methodref          #41.#42        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #8 = Class              #43            // org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest
   #9 = Class              #44            // java/lang/Object
  #10 = Class              #45            // org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest$MyClass
  #11 = Utf8               MyClass
  #12 = Utf8               InnerClasses
  #13 = Utf8               <init>
  #14 = Utf8               ()V
  #15 = Utf8               Code
  #16 = Utf8               LineNumberTable
  #17 = Utf8               LocalVariableTable
  #18 = Utf8               this
  #19 = Utf8               Lorg/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest;
  #20 = Utf8               test0
  #21 = Utf8               r
  #22 = Utf8               Ljava/lang/Runnable;
  #23 = Utf8               RuntimeVisibleAnnotations
  #24 = Utf8               Lorg/junit/Test;
  #25 = Utf8               test1
  #26 = Utf8               lambda$test1$0
  #27 = Utf8               SourceFile
  #28 = Utf8               LambdaBytecodeTest.java
  #29 = NameAndType        #13:#14        // "<init>":()V
  #30 = Utf8               BootstrapMethods
  #31 = MethodHandle       #6:#46         // invokestatic java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
  #32 = MethodType         #14            //  ()V
  #33 = MethodHandle       #6:#47         // invokestatic org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest$MyClass.printMessage:()V
  #34 = NameAndType        #48:#49        // run:()Ljava/lang/Runnable;
  #35 = Class              #50            // java/lang/Runnable
  #36 = NameAndType        #48:#14        // run:()V
  #37 = MethodHandle       #6:#51         // invokestatic org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest.lambda$test1$0:()V
  #38 = Class              #52            // java/lang/System
  #39 = NameAndType        #53:#54        // out:Ljava/io/PrintStream;
  #40 = Utf8               hello world
  #41 = Class              #55            // java/io/PrintStream
  #42 = NameAndType        #56:#57        // println:(Ljava/lang/String;)V
  #43 = Utf8               org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest
  #44 = Utf8               java/lang/Object
  #45 = Utf8               org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest$MyClass
  #46 = Methodref          #58.#59        // java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
  #47 = Methodref          #10.#60        // org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest$MyClass.printMessage:()V
  #48 = Utf8               run
  #49 = Utf8               ()Ljava/lang/Runnable;
  #50 = Utf8               java/lang/Runnable
  #51 = Methodref          #8.#61         // org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest.lambda$test1$0:()V
  #52 = Utf8               java/lang/System
  #53 = Utf8               out
  #54 = Utf8               Ljava/io/PrintStream;
  #55 = Utf8               java/io/PrintStream
  #56 = Utf8               println
  #57 = Utf8               (Ljava/lang/String;)V
  #58 = Class              #62            // java/lang/invoke/LambdaMetafactory
  #59 = NameAndType        #63:#66        // metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
  #60 = NameAndType        #67:#14        // printMessage:()V
  #61 = NameAndType        #26:#14        // lambda$test1$0:()V
  #62 = Utf8               java/lang/invoke/LambdaMetafactory
  #63 = Utf8               metafactory
  #64 = Class              #69            // java/lang/invoke/MethodHandles$Lookup
  #65 = Utf8               Lookup
  #66 = Utf8               (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
  #67 = Utf8               printMessage
  #68 = Class              #70            // java/lang/invoke/MethodHandles
  #69 = Utf8               java/lang/invoke/MethodHandles$Lookup
  #70 = Utf8               java/lang/invoke/MethodHandles
{
  public org.example.learn.java.spec.lambda.bytecode.LambdaBytecodeTest();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 5: 0
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0       5     0  this   Lorg/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest;

  public void test0();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=2, args_size=1
         0: invokedynamic #2,  0              // InvokeDynamic #0:run:()Ljava/lang/Runnable;
         5: astore_1
         6: aload_1
         7: invokeinterface #3,  1            // InterfaceMethod java/lang/Runnable.run:()V
        12: return
      LineNumberTable:
        line 15: 0
        line 16: 6
        line 17: 12
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      13     0  this   Lorg/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest;
            6       7     1     r   Ljava/lang/Runnable;
    RuntimeVisibleAnnotations:
      0: #24()

  public void test1();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=2, args_size=1
         0: invokedynamic #4,  0              // InvokeDynamic #1:run:()Ljava/lang/Runnable;
         5: astore_1
         6: aload_1
         7: invokeinterface #3,  1            // InterfaceMethod java/lang/Runnable.run:()V
        12: return
      LineNumberTable:
        line 21: 0
        line 22: 6
        line 23: 12
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      13     0  this   Lorg/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest;
            6       7     1     r   Ljava/lang/Runnable;
    RuntimeVisibleAnnotations:
      0: #24()
}
SourceFile: "LambdaBytecodeTest.java"
InnerClasses:
     public static final #65= #64 of #68; //Lookup=class java/lang/invoke/MethodHandles$Lookup of class java/lang/invoke/MethodHandles
BootstrapMethods:
  0: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
    Method arguments:
      #32 ()V
      #33 invokestatic org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest$MyClass.printMessage:()V
      #32 ()V
  1: #31 invokestatic java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
    Method arguments:
      #32 ()V
      #37 invokestatic org/example/learn/java/spec/lambda/bytecode/LambdaBytecodeTest.lambda$test1$0:()V
      #32 ()V

```






## java.lang.invoke.LambdaMetafactory.metafactory

java.lang.invoke.LambdaMetafactory.metafactory方法添加注释
```text
public static CallSite metafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType,  
                                   MethodType samMethodType, MethodHandle implMethod, MethodType instantiatedMethodType) throws LambdaConversionException {
                       }
                       
javadoc中关于formal parameter的说明:                       
caller
Represents a lookup context with the accessibility privileges of the caller. When used with invokedynamic, this is stacked automatically by the VM. 
(VM自动向operand stack自动添加, 该变量表示上下文)

invokedName
The name of the method to implement. When used with invokedynamic, this is provided by the NameAndType of the InvokeDynamic structure and is stacked automatically by the VM. 
(VM自动向operand stack自动添加, 该变量表示被实现的functional method)

invokedType
The expected signature of the CallSite. The parameter types represent the types of capture variables; the return type is the interface to implement. 
When used with invokedynamic, this is provided by the NameAndType of the InvokeDynamic structure and is stacked automatically by the VM. 
In the event that the implementation method is an instance method and this signature has any parameters, the first parameter in the invocation signature must correspond to the receiver. 
(VM自动向operand stack自动添加, 该变量表示CallSite)

samMethodType
Signature and return type of method to be implemented by the function object. 
(该变量表示)

implMethod
A direct method handle describing the implementation method which should be called (with suitable adaptation of argument types, return types, and with captured arguments prepended to the invocation arguments) at invocation time. 
(该变量表示functional method具体的实现,可能指向其他类的实例方法/类方法/)

instantiatedMethodType – The signature and return type that should be enforced dynamically at invocation time. This may be the same as samMethodType, or may be a specialization of it.        
(该变量表示)
               
```