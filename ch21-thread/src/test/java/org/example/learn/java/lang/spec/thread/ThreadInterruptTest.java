package org.example.learn.java.lang.spec.thread;

import org.example.learn.java.lang.spec.thread.util.LogUtils;
import org.example.learn.java.lang.spec.thread.util.ThreadUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 注意区分
 * isInterrupted()是实例方法
 * interrupted()是类方法,判断的是当前线程
 *
 * 线程在alive状态下,isInterrupted()才会返回正确的中断状态.当线程结束后,该实例方法返回的永远是false
 * interrupt会打断sleep导致的线程阻塞,线程提前解除阻塞并抛出异常,同时清除中断标识
 * interrupt会打断park导致的线程挂起,线程提前恢复调度
 * interrupt不会打断monitorenter指令导致的线程阻塞
 *
 * interrupt会打断线程进入waiting状态,即调用wait时发生中断异常;
 * 线程进入waiting状态后,中断会恢复唤醒调度重新抢锁(获得锁后抛出异常来退出wait方法)
 *
 */
public class ThreadInterruptTest {

    /**
     * 正常运行状态下(没有进入monitor/正在加锁/sleep)
     */
    @Test
    public void test0() {
        final AtomicBoolean computingStarted = new AtomicBoolean(false);

        Thread workerThread = new Thread("worker-thread"){
            @Override
            public void run() {
                System.out.println(this.getName() + "is running");
                computingStarted.set(true);

                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    // nop
                }
                LogUtils.log("thread[%s].isInterrupted() = %s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());

                // 上面仅仅是简单的计算,不受中断影响,所以这行代码只有for循环正常执行完后才会执行
                System.out.println(this.getName() + "is at the end of running");
            }
        };
        workerThread.start();

        LogUtils.log("thread[%s]等待thread[%s]开始工作", Thread.currentThread().getName(), workerThread.getName());
        while (!computingStarted.get()) {
            Thread.yield();
        }

        LogUtils.log("thread[%s]已经开始工作,再中断它", workerThread.getName());
        workerThread.interrupt();

        // 象征性的等一下,再查看状态
        try {
            TimeUnit.NANOSECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LogUtils.log("%s isInterrupted ? %s | thread is alive ? %s", workerThread.getName(), workerThread.isInterrupted(), workerThread.isAlive());
        Assert.assertTrue("isInterrupted()是实例方法,检测线程对象的interrupt-status,且调用该方法不会改变该状态位", workerThread.isInterrupted());

        try {
            workerThread.join();
            LogUtils.log("等待thread[%s] 结束后,再次查看该线程的中断状态", workerThread.getName());
            Assert.assertFalse("线程结束后,isInterrupted()返回的永远是false", workerThread.isInterrupted());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * sleep时抛出InterruptedException异常时,interrupt-status就不会再赋值了
     */
    @Test
    public void test1() {
        final AtomicBoolean workerReadyToSleep = new AtomicBoolean(false);
        final AtomicBoolean workerReadyToDie = new AtomicBoolean(false);

        Thread workerThread = new Thread("worker-thread"){
            @Override
            public void run() {
                LogUtils.log("thread[%s] is running", Thread.currentThread().getName());
                try {
                    workerReadyToSleep.set(true);
                    TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    LogUtils.log("while throwing InterruptedException, thread[%s] isInterrupted() = %s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    throw new RuntimeException(e);
                }

                // 防止线程提前结束
                while (!workerReadyToDie.get()) {
                    Thread.yield();
                }
                System.out.println(this.getName() + "is at the end of running");
            }
        };
        workerThread.start();

        try {
            LogUtils.log("thread[%s]等待thread[%s]开始sleep", Thread.currentThread().getName(), workerThread.getName());
            while (!workerReadyToSleep.get()) {
                Thread.yield();
            }

            // 象征性的等一下,再查看状态
            TimeUnit.SECONDS.sleep(1);
            LogUtils.log("thread[%s] is ready to sleep, thread[%s] try to interrupt it", workerThread.getName(), Thread.currentThread().getName());
            workerThread.interrupt();

            LogUtils.log("%s isInterrupted ? %s | thread is alive ? %s", workerThread.getName(), workerThread.isInterrupted(), workerThread.isAlive());
            Assert.assertFalse("thread在sleep时被interrupt,线程抛出InterruptedException表示sleep是被中断而结束的,此时interrupt-status不会赋值(抛异常已经表示了中断行为,再赋值就重复了)", workerThread.isInterrupted());

            LogUtils.log("thread[%s] 通知 thread[%s]可以结束了", Thread.currentThread().getName(), workerThread.getName());
            workerReadyToDie.set(true);
            // 象征性的等一下,再查看状态
            TimeUnit.SECONDS.sleep(1);
            Assert.assertFalse("thread在sleep时被interrupt,线程抛出InterruptedException表示sleep是被中断而结束的,此时interrupt-status不会被清空", workerThread.isInterrupted());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * interrupt线程,并不会影响获取synchronized的monitor
     */
    @Test(timeout = 5 * 1000)
    public void test21() throws InterruptedException {
        final Object lock = new Object();

        Thread workerThread = new Thread("worker-thread"){
            @Override
            public void run() {
                try {
                    Thread.currentThread().interrupt();

                    synchronized (lock) {
                        LogUtils.log("中断线程thread[%s],并不会影响其获取synchronized monitor", Thread.currentThread().getName());
                        ThreadUtils.yieldWait(2, TimeUnit.SECONDS);
                    }
                    LogUtils.log("thread[%s]结束", Thread.currentThread().getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        workerThread.start();
        LogUtils.log("begins to join thread[%s]", workerThread.getName());
        long start = System.currentTimeMillis();
        workerThread.join();
        LogUtils.log("complete to join thread[%s]", workerThread.getName());
        Assert.assertTrue("interrupt不会打断monitorenter指令导致的线程阻塞", ((System.currentTimeMillis() - start) >= TimeUnit.SECONDS.toMillis(2)));
    }

    /**
     * 验证thread因为monitor而被blocked时, 通过interrupt也无法中断阻塞状态
     */
    @Test
    public void test22() throws InterruptedException {
        final Object lock = new Object();

        Thread workerThread;
        synchronized (lock) {
            LogUtils.log("thread has got synchronized lock");

            workerThread = new Thread(() -> {
                    LogUtils.log("before acquiring monitor, thread[%s] is running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    synchronized (lock) {
                        LogUtils.log("thread[%s] has got synchronized lock  | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    }

                    LogUtils.log("thread[%s] is at the end of running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            }, "worker-thread");
            workerThread.start();


            LogUtils.log("before interrupt thread[%s], thread[%s] status is %s", workerThread.getName(), workerThread.getName(), workerThread.getState());
            // 通过interrupt worker-thread,也不会结束worker-thread的阻塞状态,最终触发junit的timeout机制
            workerThread.interrupt();
            LogUtils.log("after interrupt thread[%s], thread[%s] status is %s", workerThread.getName(), workerThread.getName(), workerThread.getState());

            ThreadUtils.yieldWait(5, TimeUnit.SECONDS);
            Assert.assertTrue("在当前线程释放锁之前,worker-thread被monitorenter阻塞", Thread.State.BLOCKED.equals(workerThread.getState()));
            LogUtils.log("thread[%s] begin to release synchronized lock", Thread.currentThread().getName());
        }
        LogUtils.log("thread[%s] has release synchronized lock", Thread.currentThread().getName());
        ThreadUtils.yieldWait(100, TimeUnit.MICROSECONDS);
        Assert.assertTrue("worker-thread被monitorenter阻塞", !Thread.State.BLOCKED.equals(workerThread.getState()));
    }

    /**
     * 获取内部锁失败 线程进入entry-queue(入口队列)
     * wait() -> 线程进入 wait-queue(等待队列)
     * notify/notifyAll -> 线程从 wait-queue重新放入entry-queue
     */
    @Test
    public void test23() throws InterruptedException {
        final Object lock = new Object();

        Thread workerThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    LogUtils.log("begin wait()");
                    lock.wait();
                    LogUtils.log("finish wait()");
                } catch (InterruptedException e) {
                    Assert.assertFalse("抛出异常时,需要清除中断位", Thread.currentThread().isInterrupted());
                    LogUtils.log("线程因为中断而提前恢复调度,并抢锁成功");
                }
            }
            LogUtils.log("thread is at the end of running | isInterrupted=%s", Thread.currentThread().isInterrupted());
        }, "worker-thread");
        workerThread.start();

        // 等worker-thread已经跑起来了
        ThreadUtils.yieldWait(5, TimeUnit.SECONDS);
        Assert.assertEquals("workerThread释放锁而进入等待队列而非入口队列,只有重新进入入口队列才有可能被重新选中恢复调度", Thread.State.WAITING, workerThread.getState());


        long start = System.currentTimeMillis();
        workerThread.interrupt();
        while (!Thread.State.WAITING.equals(workerThread.getState())) {
            Thread.yield();
        }
        Assert.assertTrue("", ((System.currentTimeMillis() - start) <= TimeUnit.MILLISECONDS.toMillis(100)));

        // 防止mainThread提前被junit回收
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
    }

    /**
     * 注意: interrupt是用来停止线程阻塞状态的
     * thread在wait前被interrupt,调用wait会抛出中断异常(在调用wait时,线程是已经持有锁的)
     */
    @Test
    public void test241() throws InterruptedException {
        final Object lock = new Object();

        Thread workerThread;
        synchronized (lock) {
            LogUtils.log("thread has got synchronized lock");

            workerThread = new Thread(() -> {
                LogUtils.log("before acquiring monitor, thread[%s] is running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                synchronized (lock) {
                    LogUtils.log("thread[%s] has got synchronized lock  | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    // 很长时间占用monitor
                    ThreadUtils.yieldWait(50, TimeUnit.SECONDS);
                }
                LogUtils.log("thread[%s] has release synchronized lock | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                LogUtils.log("thread[%s] is at the end of running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            }, "worker-thread");
            workerThread.start();

            // 等worker-thread已经跑起来了
            ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            Assert.assertTrue("在当前线程释放锁之前,worker-thread被monitorenter阻塞", Thread.State.BLOCKED.equals(workerThread.getState()));

            // 准备在wait释放monitor前,中断自己
            Thread.currentThread().interrupt();
            LogUtils.log("finish to interrupt self", Thread.currentThread().getName());
            LogUtils.log("begin wait()");
            long start = System.currentTimeMillis();
            try {
                lock.wait();
            } catch (Exception e) {
                Assert.assertTrue("线程是已经持有锁且已经被中断,在调用wait时直接抛出中断异常", ((System.currentTimeMillis() - start) <= TimeUnit.MILLISECONDS.toMillis(10)));
                Assert.assertTrue("(interrupt是用来停止线程阻塞状态的)如果在wait前被中断,调用wait将抛出中断异常(表示因为中断无法进入等待)", e instanceof InterruptedException);
            }
            LogUtils.log("complete wait()");


            LogUtils.log("thread[%s] begin to release synchronized lock", Thread.currentThread().getName());
        }
        LogUtils.log("thread[%s] has release synchronized lock", Thread.currentThread().getName());
    }

    /**
     * 注意: interrupt是用来停止线程阻塞状态的
     * thread在wait后(即已经调用wait,还没从wait返回,此时没持有锁)被interrupt,返回wait时会抛出中断异常(返回wait时线程是持有锁的)
     */
    @Test
    public void test242() throws InterruptedException {
        final Object lock = new Object();

        final Thread mainThread = Thread.currentThread();
        Thread workerThread;
        synchronized (lock) {
            LogUtils.log("thread has got synchronized lock");

            workerThread = new Thread(() -> {
                LogUtils.log("before acquiring monitor, thread[%s] is running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                synchronized (lock) {
                    LogUtils.log("thread[%s] has got synchronized lock  | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    // 当持有锁时,mainThread已经通过wait释放锁了
                    mainThread.interrupt();
                    // 只要无法获得锁,mainThread即使已经被中断,此时因为阻塞被挂起无法被cpu调度,也就无法抛出异常
                    ThreadUtils.yieldWait(5, TimeUnit.SECONDS);
                    Assert.assertTrue("只要无法获得锁,mainThread即使已经被中断,还保持WAITING的状态,无法抛出异常", !Thread.State.WAITING.equals(mainThread.getState()));
                }
                LogUtils.log("thread[%s] has release synchronized lock | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                LogUtils.log("thread[%s] is at the end of running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            }, "worker-thread");
            workerThread.start();

            // 等worker-thread已经跑起来了
            ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            Assert.assertTrue("在当前线程释放锁之前,worker-thread被monitorenter阻塞", Thread.State.BLOCKED.equals(workerThread.getState()));

            // 通过wait释放锁
            LogUtils.log("begin wait()");
            try {
                long start = System.currentTimeMillis();
                lock.wait();
                Assert.assertTrue("在获得锁之前无法退出wait方法.当获得锁时且在退出wait方法前再检查一下中断位,如果没有中断就正常退出,否则以抛异常的形式退出", ((System.currentTimeMillis() - start) >= TimeUnit.SECONDS.toMillis(5)));
            } catch (Exception e) {
                Assert.assertTrue("(interrupt是用来停止线程阻塞状态的)如果在wait后被中断,获得锁后将以抛出中断异常的形式退出wait(表示等待中曾发生中断)", e instanceof InterruptedException);
            }
            LogUtils.log("complete wait()");

            LogUtils.log("thread[%s] begin to exit the sync block", Thread.currentThread().getName());
        }
        LogUtils.log("thread[%s] has exited the sync block", Thread.currentThread().getName());
        ThreadUtils.yieldWait(100, TimeUnit.MICROSECONDS);
        Assert.assertTrue("worker-thread被monitorenter阻塞", !Thread.State.BLOCKED.equals(workerThread.getState()));
    }


    /**
     * park()会因为interrupt而提前结束
     * 且park()不会自动清除中断标志
     */
    @Test
    public void test31() {
        // 先中断自己
        Thread.currentThread().interrupt();
        long start = System.currentTimeMillis();
        // 再park
        LogUtils.log("开始park thread[%s] (isInterrupted=%s) at %s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertTrue(Thread.currentThread().isInterrupted());
        LockSupport.park(); //应该无限期挂起而无法执行下面的方法
        LogUtils.log("结束park thread[%s] (isInterrupted=%s) at %s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertTrue("在park()前或者中,中断当前线程,会导致park直接退出", ((System.currentTimeMillis() - start) < TimeUnit.MILLISECONDS.toMillis(100)));
        Assert.assertTrue("park()不会自动清除中断标志", Thread.currentThread().isInterrupted());
    }
}
