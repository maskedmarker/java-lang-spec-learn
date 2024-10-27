package org.example.learn.java.lang.spec.other.serialize;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * 被序列化的类
 * <p>
 * note: 通过反序列化来实例化对象的时候,
 * Constructor是反射时的用户侧的接口类,实际用来生成heap上实例的类是ConstructorAccessor
 * 反序列化时,java标准库根据序列化文件的对象信息,手工构造了一个SerializationConstructorAccessorImpl(即ConstructorAccessor子类),该子类可以在heap上创建一个待反序列化的实例.
 * 该实例的字段只有初始化默认值了,并不会有类似于<init>方法的赋值过程.
 * 具体的java8中的实现
 * java.io.ObjectInputStream#readObject
 * 	java.io.ObjectInputStream#readOrdinaryObject
 * 		java.io.ObjectInputStream#readClassDesc
 * 			java.io.ObjectInputStream#readNonProxyDesc
 * 				java.io.ObjectStreamClass#initNonProxy
 * 					java.io.ObjectStreamClass#lookup(java.lang.Class<?>, boolean)
 * 						java.io.ObjectStreamClass#ObjectStreamClass(java.lang.Class<?>)
 * 							sun.reflect.ReflectionFactory#newConstructorForSerialization(java.lang.Class<?>, java.lang.reflect.Constructor<?>)
 * 								sun.reflect.ReflectionFactory#generateConstructor
 * 									sun.reflect.MethodAccessorGenerator#generateSerializationConstructor
 * 										sun.reflect.MethodAccessorGenerator#generate
 * 											sun.reflect.ClassDefiner#defineClass
 * java.io.ObjectStreamClass#newInstance
 */
public class SerializableClass2 implements Serializable {

    private static final long serialVersionUID = 3408169507716120674L;

    private int id;
    private String name;
    // 序列化时间戳
    private long serialTimeMillis = -1;

    public SerializableClass2() {
        System.out.println(SerializableClass2.class.getSimpleName() + " is instantiating");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("serialTimeMillis", serialTimeMillis)
                .toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
        System.out.println(" 调用 readObject() !!!");
        // 该实例的字段只有初始化默认值了,并不会有类似于<init>方法的赋值过程
        id = in.readInt();
        name = (String) in.readObject();
        serialTimeMillis = in.readLong();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        System.out.println(" 调用 writeObject() !!!");
        out.writeInt(id);
        out.writeObject(name);
        out.writeLong(System.currentTimeMillis());
    }
}
