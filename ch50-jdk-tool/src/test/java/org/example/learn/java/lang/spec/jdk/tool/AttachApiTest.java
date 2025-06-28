package org.example.learn.java.lang.spec.jdk.tool;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

/**

VirtualMachine.list() 涉及到的关键类
sun.jvmstat.perfdata.monitor.protocol.local.MonitoredHostProvider
sun.jvmstat.perfdata.monitor.protocol.local.LocalVmManager
sun.jvmstat.perfdata.monitor.protocol.local.PerfDataFile

jvm进程在启动时,会在指定目录内写入文件
C:\Users\Administrator\AppData\Local\Temp\hsperfdata_Administrator\14588
该文件为共享内存映射文件,读取时必须确保目标 JVM 仍在运行.

VirtualMachine.attach(vmd) 涉及到的关键类
sun.tools.attach.WindowsAttachProvider
读取 hsperfdata文件中的特定数据来判断是否可以attachable
 sun.rt.jvmCapabilities



 */
public class AttachApiTest {

    @Test
    public void test() {

        List<VirtualMachineDescriptor> list = VirtualMachine.list();

        for (VirtualMachineDescriptor vmd : list) {
            System.out.println("Found JVM: " + vmd.displayName() + " (PID: " + vmd.id() + ")");

            try {
                VirtualMachine vm = VirtualMachine.attach(vmd);

                // 获取系统属性
                Properties props = vm.getSystemProperties();
                System.out.println("java.home: " + props.getProperty("java.home"));
                System.out.println("java.version: " + props.getProperty("java.version"));

                vm.detach();
            } catch (Exception e) {
                System.out.println("Cannot attach to JVM: " + vmd.id());
                e.printStackTrace();
            }

            System.out.println("------------");
        }
    }
}
