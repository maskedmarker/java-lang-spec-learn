package org.example.learn.java.lang.spec.other.serialize;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 被序列化的类
 * <p>
 * note: 通过反序列化来实例化对象的时候,
 * 对于实现了Externalizable接口的类,在反序列化时,直接使用该类的无参构造函数来实现在heap上创建实例,并触发<init>方法的赋值
 */
public class SerializableClass3 implements Externalizable {

    private static final long serialVersionUID = 3408169507716120674L;

    private int id;
    private String name;
    // 序列化时间戳
    private long serialTimeMillis = -1;

    public SerializableClass3() {
        System.out.println(SerializableClass3.class.getSimpleName() + " is instantiating");
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

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        System.out.println(" 调用 readExternal() !!!");
        // 该实例的字段已经初始化
        id = in.readInt();
        name = (String) in.readObject();
        serialTimeMillis = in.readLong();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        System.out.println(" 调用 writeExternal() !!!");
        out.writeInt(id);
        out.writeObject(name);
        out.writeLong(System.currentTimeMillis());
    }
}
