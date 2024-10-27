package org.example.learn.java.lang.spec.opcode;

public class NewInstanceOpcodeTest {

    /**
     * 构造函数对应的字节码.
     * invokespecial <init> 初始化对象内存后

      public void test0();
        descriptor: ()V
        flags: ACC_PUBLIC
        Code:
          stack=2, locals=2, args_size=1
             0: new           #2                  // class org/example/learn/java/lang/spec/opcode/MyObject
             3: dup
             4: invokespecial #3                  // Method org/example/learn/java/lang/spec/opcode/MyObject."<init>":()V
             7: astore_1
             8: return
          LineNumberTable:
            line 6: 0
            line 7: 8
          LocalVariableTable:
            Start  Length  Slot  Name   Signature
                0       9     0  this   Lorg/example/learn/java/lang/spec/opcode/NewInstanceOpcodeTest;
                8       1     1 myObject   Lorg/example/learn/java/lang/spec/opcode/MyObject;

     */
    public void test0() {
        MyObject myObject = new MyObject();
    }
}
