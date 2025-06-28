package org.example.learn.java.lang.spec.jdk.tool;

import org.junit.Test;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.Units;
import sun.jvmstat.perfdata.monitor.AbstractPerfDataBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 从hsperfdata文件读取信息
 */
public class SimplePerfDataReaderTest {

    /**
     * 不要试图手动解析hsperfdata,使用jdk自带的现成的工具类来完成
     */
    public static class MyPerfDataBuffer extends AbstractPerfDataBuffer {

        public MyPerfDataBuffer(ByteBuffer buffer) throws MonitorException {
            super();
            super.createPerfDataBuffer(buffer, -1);
        }
    }

    private static void printMonitor(Monitor monitor) {
        System.out.print(monitor.getName());
        System.out.print(" = ");
        System.out.print(monitor.getValue());

        Units unit = monitor.getUnits();

        if ((unit.intValue() != Units.NONE.intValue()) && (unit.intValue() != Units.STRING.intValue())) {
            System.out.print(" (");
            System.out.print(monitor.getUnits());
            System.out.print(")");
        }

        System.out.println();
    }

    @Test
    public void test() throws Exception {
        // 替换为你的实际路径和 pid
        String pid = "20280"; // 你的 Java 进程 pid
        String filePath = System.getProperty("java.io.tmpdir") + "/hsperfdata_" + System.getProperty("user.name") + "/" + pid;
        System.out.println("filePath = " + filePath);

        // 不要使用: FileChannel fc = new FileInputStream(filePath).getChannel() 因为无法控制读写权限
        try (FileChannel ch = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ)) {
            ByteBuffer buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size());
            MyPerfDataBuffer perfReaderBuffer = new MyPerfDataBuffer(buffer);
            perfReaderBuffer.findByPattern(".*").forEach(SimplePerfDataReaderTest::printMonitor);
        }
    }
}
