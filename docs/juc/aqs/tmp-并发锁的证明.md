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
