# nio-sendfile


## 典型使用场景

```text
sendfile适用场景:
静态文件服务器(HTML/JS/图片/视频)
CDN边缘节点
大文件下载
对CPU敏感的高并发服务

----------------------------------------

sendfile不适用场景:
SSL/TLS加密(Linux部分支持内核态TLS加密)
小批量读取 (适用传统的系统调用read,或者mmap)
非连续访问

系统调用read和mmap都会触发page-fault(数据从磁盘加载到page-cache中),如果是单次读操作调用read更好,如果是多次读操作mmap的上下文切换/系统调用次数更少.如果是随机访问操作mmap更好.
```


## 传统方式 vs. sendfile

```text
传统文件发送路径(多次拷贝)
        Disk
         ↓
        Kernel page cache
         ↓ (copy)
        User buffer
         ↓ (copy)
        Kernel socket buffer
         ↓
        NIC


问题：
1. 至少2次数据拷贝
2. 2次系统调用(read + write)
3. 用户态 ↔ 内核态频繁切换
4. CPU cache pollution明显



sendfile 路径(零拷贝语义)
        Disk
         ↓
        Kernel page cache
         ↓
        Kernel socket buffer
         ↓
        NIC


特点：
1. 无用户态数据拷贝
2. 单次系统调用
3. 数据始终停留在内核态
4. CPU 开销显著降低

严格来说,大多数实现是“减少拷贝”,而非物理意义上的0次拷贝(DMA仍然存在).  (零拷贝指的是无需用户态拷贝数据)
```


## Java中的sendfile

```text
FileChannel.transferTo(position, count, socketChannel);
Params:
        position – The position within the file at which the transfer is to begin; must be non-negative  (position必须正确维护)
        count – The maximum number of bytes to be transferred; must be non-negative 
        target – The target channel
Returns:
        The number of bytes, possibly zero, that were actually transferred (基于返回值,正确维护position)


在Linux上： 内部优先使用sendfile;条件不满足时自动fallback.

Netty、Tomcat、Jetty 均大量依赖该机制.
```



## Linux sendfile的系统调用

```text
ssize_t sendfile(int out_fd, int in_fd, off_t *offset, size_t count);

参数含义：
in_fd：源文件描述符(必须是普通文件)
out_fd：目标 socket 描述符
offset：文件偏移(可选,NULL 表示使用当前偏移)
count：发送的最大字节数

返回值：
实际发送的字节数
0：EOF
-1：错误(如 EAGAIN/EINVAL)

文件类型限制
in_fd 必须是普通文件,不支持 pipe/socket → socket(早期内核).Linux 4.x 以后支持 splice/copy_file_range 等更通用方案.

TLS/HTTPS 场景
sendfile无法直接用于HTTPS,原因：TLS加密在用户态完成.
解决方案：sendfile + kTLS(Linux 4.13+),或退化为 read+write
```



## Linux sendfile的系统调用详情

```text
系统调用入口 → VFS → page cache → socket → TCP → NIC 的完整路径,按 Linux 内核真实执行顺序,详细说明一次 sendfile() 调用在内核内部到底发生了什么.
说明基于现代 Linux(4.x–6.x),忽略与主线无关的历史实现差异.

-------------------------------------------------------------------------------
一、系统调用入口(userspace → kernel)

1. 用户态调用
sendfile(out_fd, in_fd, &offset, count)

CPU 发生：
syscall/sysenter
切换到内核态
保存寄存器、切换到内核栈

2. sys_sendfile64
内核入口函数(简化)： SYSCALL_DEFINE4(sendfile64, int, out_fd, int, in_fd, loff_t __user *, offset, size_t, count)

主要工作：
fdget() 获取 struct file *in_file
fdget() 获取 struct file *out_file
校验：
    in_file 是否可读
    out_file 是否可写
    out_file 是否是 socket



-------------------------------------------------------------------------------
二、VFS 层：sendfile 的分发

3. 调用 do_sendfile()
do_sendfile(out_file, in_file, offset, count)

职责：
决定使用哪种发送路径
检查文件与 socket 的操作表

关键判断：
if (!out_file->f_op->sendpage)
    fallback_to_read_write();

这里的 sendpage 是零拷贝的关键.



-------------------------------------------------------------------------------
三、核心机制：sendpage(零拷贝的核心)

4. sendpage 是什么？
ssize_t (*sendpage)(struct socket *sock, struct page *page, int offset, size_t size, int flags);

含义： 把 page cache 中的一页“挂接”到 socket buffer,而不是拷贝数据

这是 sendfile 能避免用户态拷贝的根本原因.



-------------------------------------------------------------------------------
四、page cache 阶段(文件 → 内存页)

5. 查找/加载 page cache

对 in_file：
1. 根据 offset 计算页号.
2. 从 page cache(radix tree/xarray)查找.
3. 如果 page 不存在：
        触发同步或异步磁盘 I/O
        文件系统(ext4/xfs)执行：
            submit_bio
            块层调度
            磁盘 DMA → page cache

关键点：
磁盘数据永远只进入 page cache,没有用户缓冲区参与



-------------------------------------------------------------------------------
五、socket buffer 组装(最关键部分)

6. sock_sendpage()
VFS 调用 socket 层： 
        sock_sendpage(sock, page, offset, size, flags);
进入： 
        inet_sendpage()
再进入 TCP： 
        tcp_sendpage()


7. 创建skb(socket buffer)

TCP 层创建 struct sk_buff： 
        skb = alloc_skb(...)
但重点是：skb不复制page 数据,skb的frags[]直接引用page cache

skb_add_rx_frag(skb, frag_idx, page, offset, size);

结果：
skb 持有的是：
    struct page *
    offset
    length
引用计数：
    get_page(page) 增加 page refcount
    防止 page 被回收

    
8. skb 进入发送队列
tcp_queue_skb()
此时：
    skb 挂到 socket 的 send queue
    受 TCP flow control 约束
    若 buffer满,则阻塞或 EAGAIN    



-------------------------------------------------------------------------------
六、TCP 层处理(协议栈)

9. TCP segmentation(可能)
如果 page 较大：
    TCP 根据 MSS 拆分 skb
    仍然 共享 page
    不发生数据复制
    

10. TCP output
tcp_transmit_skb()

执行：
    TCP header 填充
    checksum(可能 offload)
    调用 IP 层    



-------------------------------------------------------------------------------
七、IP → 网络设备 → NIC

11. IP 层
路由查找
填充IP header


12. Qdisc/netdev
dev_queue_xmit(skb)

skb 进入 qdisc
调用网卡驱动


13. DMA 到网卡(真正的“零拷贝”终点)

网卡驱动：
    使用 DMA
    直接从 page cache 对应物理页
    将数据搬到 NIC buffer
CPU 不参与数据搬运



-------------------------------------------------------------------------------
八、发送完成与资源释放

14. ACK 到达后
skb 被释放
put_page(page) 减少引用计数
page 仍留在 page cache(供复用)



-------------------------------------------------------------------------------
九、关键点总结(非常重要)


数据拷贝次数
阶段	                      是否拷贝
磁盘 → page cache	      DMA
page cache → skb	      ❌
skb → NIC	              DMA

无 CPU memcpy


为什么必须是“文件 → socket”
原因：
    文件有稳定的 page cache
    socket 支持 sendpage
    pipe/socket → socket 早期不支持
    

sendfile的本质一句话: sendfile不是“把数据拷贝到 socket”,而是“让 socket 引用文件页”.   
-------------------------------------------------------------------------------
十、常见误区澄清

❌ “sendfile 是 mmap + write”
错误: sendfile 不映射用户地址空间


❌“sendfile 一定是 0 copy”
不严格,DMA 仍存在,但 CPU 不拷贝



-------------------------------------------------------------------------------
在典型 Linux TCP 路径中(高度概括)：
sys_sendfile
  └─ do_sendfile
      └─ sock_sendpage
          └─ inet_sendpage
              └─ tcp_sendpage
                  ├─ 构造 skb(引用 page cache)     ♥️♥️♥️
                  ├─ tcp_queue_skb()              💯💯💯将数据封装成skb,entail进入socket的发送队列
                  ├─ tcp_push()                 (判断当前socket状态,是否应该立即把socket的发送队列中的skb发出去)
                  │   └─tcp_transmit_skb()      (真正进入协议栈)
                  │     └─ip_queue_xmit()
                  │         └─dev_queue_xmit()
                  └─ return 已发送字节数            (返回的字节数指的是放入到发送队列中的数据的字节数)
                  
TCP 是异步协议栈.
sendfile 做的是：把数据“提交”给 TCP,而不是“执行发送动作”. 
但注意：并非“无条件立刻 return”.
如果是阻塞socket,当发送队列满了,tcp_queue_skb就会发生阻塞,此时不会返回.
如果是非阻塞socket,当发送队列满了,tcp_queue_skb不会发生阻塞,此时返回异常码(EAGAIN/EWOULDBLOCK).

       
-------------------------------------------------------------------------------
```

## sendfile 与其他“零拷贝”技术对比

```text
技术	                  典型用途	                           特点
sendfile	          文件 → socket	                       最简单、最成熟
mmap	              文件映射	                           仍需一次拷贝
splice	              fd → fd	                           通用但复杂
copy_file_range	      文件 → 文件	                       文件系统级优化
DPDK	              用户态网络	                           绕过内核,成本高
```

## (扩展阅读) Qdisc(Queueing Discipline)

```text
Qdisc(Queueing Discipline,排队规则)是 Linux 内核网络子系统中的一个重要组件,它主要负责管理网络数据包的排队和调度,以确保数据包能够高效、有序地从网络设备发送出去.


二、Qdisc 的工作原理

1. 数据包入队

当网络设备(如网卡)接收到一个数据包时,它会将数据包传递给Qdisc. Qdisc根据配置的调度算法将数据包放入相应的队列中.
例如,如果使用的是先进先出(FIFO)调度算法,数据包将按照到达的顺序依次入队.

2. 数据包调度

Qdisc 根据配置的调度算法从队列中取出数据包进行处理.不同的调度算法有不同的处理方式,如优先级调度、加权公平队列调度等.
例如,在优先级调度算法中,Qdisc 会优先处理高优先级队列中的数据包.如果高优先级队列为空,则处理低优先级队列中的数据包.

3. 数据包出队

经过调度后,Qdisc 将数据包传递给网络设备(如网卡)进行发送.网卡通过 DMA(Direct Memory Access,直接内存访问)将数据包从内存中读取到网卡的发送缓冲区,然后发送到网络中.
例如,当 Qdisc 从队列中取出一个数据包后,它会将数据包的指针传递给网卡驱动.网卡驱动会使用 DMA 将数据包从内存中读取到网卡的发送缓冲区,然后发送到网络中.
```