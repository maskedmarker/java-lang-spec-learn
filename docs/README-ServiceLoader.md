# 关于ServiceLoader的一些记录

## 参考资料
DriverManager的源码中有关于ServiceLoader使用的代码,可以作为非常好的参考对象.


## 关键点:
### java.util.ServiceLoader#load(java.lang.Class<S>)方法
Creates a new service loader for the given service type.

1. 这里的入参通常是interface
2. 该方法也不会触发接口实现类的类初始化.
3. LazyIterator用来解析service文件内容/接口实现类的类初始化/实例化接口实现类的实例.
4. LazyIterator使用java.lang.ClassLoader.getResources在classpath下的资源(文件路径使用的是相对路径,即META-INF/services/service类名)
5. java.util.ServiceLoader.LazyIterator#nextService方法中的实例化接口实现类的实例时,会触发接口实现类的类初始化

DriverManager的源码
```java
public class DriverManager {
    // ...
    private static void loadInitialDrivers() {
        // ...
        ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
        Iterator<Driver> driversIterator = loadedDrivers.iterator();
        try{
            while(driversIterator.hasNext()) {
                // 这里会触发类的初始化(比如:可以让Driver实现类将自己注册到DriverManager)
                driversIterator.next();
            }
        } catch(Throwable t) {
            // Do nothing
        }
        
        // ...
        String[] driversList = drivers.split(":");
        println("number of Drivers:" + driversList.length);
        for (String aDriver : driversList) {
            try {
                println("DriverManager.Initialize: loading " + aDriver);
                // 类的初始化已经完成了,这里主要是为了打印日志,说明用户设置的系统参数jdbc.drivers中指定的Driver是否初始化成功
                Class.forName(aDriver, true, ClassLoader.getSystemClassLoader());
            } catch (Exception ex) {
                println("DriverManager.Initialize: load failed: " + ex);
            }
        }
    }    
    // ...
}				
```