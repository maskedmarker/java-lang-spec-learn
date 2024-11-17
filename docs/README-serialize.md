## 对象创建方式
1. 通过new关键字创建对象
2. 通过反射,使用构造函数创建对象
3. 通过反序列化来创建对象(对象实例不通过构造函数,可以在对应的构造函数上放置断点验证)

## 反序列化

### 参考文章
1. https://howtodoinjava.com/java/serialization/how-deserialization-process-happen-in-java/
2. javadoc
   java.io.ObjectStreamClass.newInstance
   Creates a new instance of the represented class.
   If the class is externalizable, invokes its public no-arg constructor; otherwise, if the class is serializable, invokes the no-arg constructor of the first non-serializable superclass.
