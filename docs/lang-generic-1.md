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