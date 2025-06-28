package org.example.learn.java.lang.spec.jdk.tool;

import org.junit.Test;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;


public class HsPerfDataApiTest {

    @Test
    public void test() throws Exception {
        int pid = 20280; // 替换为目标 JVM 的 PID

        MonitoredHost host = MonitoredHost.getMonitoredHost("localhost");
        MonitoredVm vm = host.getMonitoredVm(new VmIdentifier("//" + pid));

        String vmVersion = MonitoredVmUtil.vmVersion(vm);
        System.out.println("vmVersion = " + vmVersion);

        host.detach(vm);
    }
}
