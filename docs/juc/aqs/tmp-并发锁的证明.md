# 并发锁的证明


## 常见的证明思路

```text
To prove that a lock is correct, you must show that it satisfies the essential correctness properties of mutual exclusion algorithms. In academic and industrial concurrency research, a lock is considered correct only if all of the following properties hold:

✔ 1. Mutual Exclusion (互斥性)

At any time, at most one thread can hold the lock.
Formally:
    ∀ times t: (#threads_in_critical_section(t) ≤ 1)

How to prove:
    Show that the lock grants ownership exclusively.
    Show that the entry protocol prevents two threads from reaching the critical section simultaneously (e.g., via CAS, queue, state flag, etc.).
    Use invariant: state == FREE <-> no thread owns the lock.



✔ 2. Deadlock Freedom (无死锁)

If some threads want the lock, eventually some thread will succeed.

This is weaker than fairness — it only requires global progress.

How to prove:
    Show that no circular wait can arise.
    Show that there is no infinite waiting due to self-blocking or state that cannot be recovered.

Typically demonstrated by proving that the algorithm cannot reach a state in which:
    lock is owned
    no owner can release it
    but others are still waiting



✔ 3. Starvation Freedom (无饥饿)

Every thread that tries to acquire the lock infinitely often will eventually succeed.

How to prove:
    Identify the guarantee of fair ordering (FIFO queue, ticket ordering, CLH/MCS queue).
    Show that a thread cannot be bypassed indefinitely.
    Show the ordering is total and finite.

Note: Some locks intentionally do not guarantee starvation freedom (e.g., spin locks using TAS).



✔ 4. Bounded Waiting / FIFO Ordering (有界等待 / 顺序性)

Not required for correctness, but many locks provide it.

Proof strategy:
    Show that the number of times other threads can enter the CS before a waiting thread enters is bounded.
    For queue-based locks (CLH/MCS), show monotonic progress along the queue.



✔ 5. Safety & Liveness Properties Mapping
Property Type	             Meaning	                                Typical Proof Method
Safety	                     Nothing bad ever happens	                Show invariant is always preserved
Liveness	                 Something good eventually happens	        Show progress measure decreases or eventually unblocks




✔ 6. Common Proof Techniques
Technique	                                 When Used
Invariant (不变量)	                         Prove mutual exclusion
Induction (数学归纳)                           Show invariants hold across steps
Sequential consistency mapping	             Prove linearization points
Wait-for graph	                             Prove deadlock-freedom
Ranking function (度量函数)	                 Prove liveness / bounded waiting
Linearization point argument	             For correctness of memory effects




✔ Example — CLH Lock Correctness Sketch

Mutual exclusion:
    A thread enters CS only after its predecessor writes status = available.
    Two nodes cannot observe predecessor available simultaneously → mutex.

Deadlock-free:
    Each thread actively updates its predecessor’s status → no circular wait.
    The predecessor always transitions to available or leaving, not permanent stall.

Starvation-free:
    FIFO queue ensures each node eventually becomes head → finite precedence.
    


    
🚀🚀🚀🚀🚀🚀 Key Insight 💯💯💯💯💯💯💯

Lock correctness is not about “no bug occurs in tests”;It is about formal reasoning over all possible executions.    

To prove a lock correct, you must define:
    State variables
    Invariants
    State transitions
    Proof that transitions preserve invariants and guarantee progress
```

```text
如下是重点结论

由于并发下有无穷的执行顺序,穷举法不好使.衍生出了通过Invariant+反证法来证明锁在并发下的正确性.

1.提前设定好Invariants 
    这些Invariants是推理的起点,用来最终证明锁正确. 💯
    这些Invariants可以是某个具体的变量,也可以是某种状态(比如队列中节点的前后顺序保持不变; 比如只有第二个节点能去尝试抢占锁; 比如节点在挂起前要保证前节点设置好了SIGNAL;比如前节点看到自己的waitStatus为SIGNAL后负责唤醒后续节点)
2. 在执行方法时,维护好Invariants
```

```text
🚀怎么证明锁本身是正确的？

核心思想：构造一个无可辩驳的论证
证明锁的正确性，本质上是构造一个逻辑论证，说明在所有可能的程序执行交错下，锁的属性都成立。由于并发执行的可能性是无穷的，我们需要一种抽象和归纳的方法。

证明框架
形式化定义：用精确的、数学化的语言定义“锁”、“持有锁”、“互斥”等概念。
识别不变量：找出一个或多个在锁算法执行过程中始终为真的条件。这是最关键的一步。💯
基于不变量进行证明：
    互斥：证明锁的不变量蕴含着“不可能有两个线程同时持有锁”。
    无死锁/无饥饿：证明任何一个试图获取锁的线程，最终都能成功。这通常需要证明存在一个“先后顺序”或“等待链”，并且这个顺序是有限的。
    
    
证明锁本身的正确性是一个严谨的、逻辑驱动的过程：
    1. 对于简单锁（如Peterson锁）：可以通过识别不变量和反证法来手工构造一个令人信服的证明。
    2. 对于复杂锁（如队列锁）：利用其显式维护的顺序来简化证明。
    3. 对于工业级、性命攸关的锁：必须依赖形式化验证工具进行机器辅助的、穷尽的检查。
这个过程的本质是，将并发程序中不确定的交错执行，通过逻辑和数学的方法，转化为一个确定的、可推理的论证。    
```