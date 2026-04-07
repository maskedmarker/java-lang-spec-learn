# nio-mmap


```text
在网络发送场景下:
mmap优化的是：read() 的拷贝,避免：内核 → 用户 buffer 的 copy
sendfile优化的是：整个 IO 路径,避免：内核 → 用户 → 内核 的来回拷贝

换句话说：mmap 只是“让你更快拿到数据”,但“发送数据”还是要走内核.



mmap在发送数据时,无法避免数据复制.

假设你用 mmap + socket 发送数据：
Step 1：mmap 映射阶段
磁盘 → 内核页缓存 → 映射到用户空间（虚拟地址）
👉注意：
没有发生数据拷贝
只是建立映射关系（页表）

Step 2：访问数据（关键）
当你访问 buf.get() 或 write 时：
如果页不在内存 → 触发 page fault 然后：磁盘 → Page Cache（加载）,此时数据在：内核页缓存（Page Cache）

Step 3：发送数据（核心问题）
调用：socketChannel.write(buf);
发生：用户态 buffer → 内核 socket buffer
👉 关键：必须 copy 一次（memcpy）
```

```text
mmap在read/write操作下,可以不触发系统调用,避免用户态/内核态切换,同时还减少了一次数据复制.
```