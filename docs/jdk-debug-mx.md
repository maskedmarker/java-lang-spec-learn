# jdk自带的底层能力


## Java Mission Control

Java Mission Control(JMC)通过 Java Flight Recorder(JFR) 与正在运行的 JVM 进程建立连接,方式主要有两种：
1. 本地 JVM 进程连接(通过 JMX)
   1. JVM 启动时,如果开启了 JMX Agent,JMC 可以直接连接. 
   2. JMC 可以通过本地协议(Attach API)建立连接
2. 远程连接
   1. 显式开启 JMX 并设置监听 IP
   2. 配置防火墙规则开放 JMX 端口
   3. 远程主机上的 JVM 开启 JFR

```text
java -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.rmi.port=9010 \
     -Djava.rmi.server.hostname=192.168.1.100 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=myrecording.jfr \
     -jar your-app.jar

如果是 Java 8,JFR 是商业特性(需要解锁).Java 11+ JFR 是开源并内置的(无须解锁).
JMC 使用的是 JMX + Attach API,所以如果用 -XX:+DisableAttachMechanism 禁用了 attach,JMC 将无法连接本地进程.
```

```text
当你在 JMC 启动界面中看到本地 JVM 列表(比如 org.springframework.Boot、MyAppMainClass),这些是 JMC 使用 Attach API 扫描到的.

具体流程：

1. 枚举本地 JVM
    JMC 使用 VirtualMachine.list() 枚举当前机器上可 attach 的所有 JVM 实例.

2. Attach 到目标进程
    选中一个 JVM 后,JMC 会使用 VirtualMachine.attach(pid) 连接到它.这个过程通过本地 socket 文件(Linux)或命名管道(Windows)通信.

3. 加载 JMX agent(如果没有启用)
    如果目标 JVM 没有开启 JMX 管理代理,JMC 会动态加载 management-agent.jar：
    vm.loadAgent("lib/management-agent.jar");
    加载完成后,就可以通过 JMX 来控制 JVM,比如访问 JFR 控制器.

4. 控制 Flight Recorder 或获取 JVM 信息
    JMC 然后通过 com.sun.management 的接口访问 JFR,比如启动/停止记录、获取线程、GC、堆信息等.
```

#### HotSpot Performance Data

C:\Users\Administrator\AppData\Local\Temp\hsperfdata_Administrator 目录下的文件是 HotSpot 性能数据文件(HotSpot Performance Data)
由 JVM 启动时自动创建,JVM 进程退出时会自动删除它对应的文件.
该文件为共享内存映射文件,读取时必须确保目标 JVM 仍在运行.

```text
C:\Users\Administrator\AppData\Local\Temp\hsperfdata_Administrator 目录下的文件是 HotSpot 性能数据文件（HotSpot Performance Data）,
由 JVM 启动时自动创建,主要用于 JVM 内部和工具（如 jps, jstat, jconsole）之间的通信.
这些文件的命名通常是 JVM 进程的 PID,例如：
C:\Users\Administrator\AppData\Local\Temp\hsperfdata_Administrator\12345

这个 12345 就是 JVM 进程的 PID,对应的数据文件包含：
JVM 启动时间戳
GC 活动信息
类加载统计
编译器行为
内存使用情况（heap/non-heap）
线程信息等

格式是二进制,主要供 Java 工具读取.

这些文件是给 JDK 自带的诊断工具使用的,例如：
jps：列出所有运行的 Java 进程
jstat：查看 GC 等运行时状态
jconsole / Java Mission Control：性能分析工具
它们都通过读取 hsperfdata_<username> 目录下的文件来发现和附加到本地 JVM.

是否可以删除这些文件？
正常来说不要手动删除运行中进程的文件,否则会导致诊断工具报错或 JVM 警告.
JVM 进程退出时会自动删除它对应的文件.
如果有残留（如 JVM 异常退出）,系统重启后清理是安全的,或者你也可以手动清理旧的文件（确认 PID 不存在即可）.
```

```text
hsperfdata 文件结构

文件结构总览（按顺序）
PerfData Header（固定长度）
ByteData Entries（一个或多个数据项）
String Table（字符串常量）



```