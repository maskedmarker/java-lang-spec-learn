# 关于java注解





## repeatable annotation
可重复注解在类型声明时必须同步声明一个该注解的容器注解.

如下代码是TaskPlan源码和编译后的字节码.
注意观察class文件的RuntimeVisibleAnnotations部分:
RuntimeVisibleAnnotations:
0: #13(#14=[@#15(#16=s#17,#18=s#19),@#15(#16=s#20,#18=s#21)])
1: #22(#14=s#23)

不可重复注解和可重复注解在class文件中的格式是不同的.可重复注解是被容纳在容器注解中的;不可重复注解不需要.


```java
public class TaskPlan {

    @Schedule(jobName = "job1", cron = "11111")
    @Schedule(jobName = "job2", cron = "22222")
    @Executor("thread-pool-task")
    public void perform() {
    }
}
```

```opcode
Constant pool:
  #13 = Utf8               Lorg/example/learn/java/lang/spec/annotation/ScheduleAnnotationContainer;
  #14 = Utf8               value
  #15 = Utf8               Lorg/example/learn/java/lang/spec/annotation/Schedule;
  #16 = Utf8               jobName
  #17 = Utf8               job1
  #18 = Utf8               cron
  #19 = Utf8               11111
  #20 = Utf8               job2
  #21 = Utf8               22222
  #22 = Utf8               Lorg/example/learn/java/lang/spec/annotation/Executor;
  #23 = Utf8               thread-pool-task

{  
  public void perform();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=0, locals=1, args_size=1
         0: return
      LineNumberTable:
        line 9: 0
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0       1     0  this   Lorg/example/learn/java/lang/spec/annotation/TaskPlan;
    RuntimeVisibleAnnotations:
      0: #13(#14=[@#15(#16=s#17,#18=s#19),@#15(#16=s#20,#18=s#21)])
      1: #22(#14=s#23)
}      
```



