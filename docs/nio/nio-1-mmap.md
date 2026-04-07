# nio-mmap


```text
mmap优化的是：read() 的拷贝,避免：内核 → 用户 buffer 的 copy
sendfile优化的是：整个 IO 路径,避免：内核 → 用户 → 内核 的来回拷贝

换句话说：mmap 只是“让你更快拿到数据”,但“发送数据”还是要走内核.
```

```text
mmap 必须进入用户态再 write()”到底是什么意思？为什么这是性能差异的核心？


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