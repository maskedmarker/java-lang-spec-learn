# juc-AQS-总结


## waitStatus

```text
// 当前线程因为超时或者中断被取消.这是一个终结态,也就是状态到此为止
static final int CANCELLED =  1;

// 当前线程的后继线程被阻塞或者即将被阻塞,当前线程释放锁或者取消后需要唤醒后继线程.这个状态一般都是后继线程来设置前驱节点的
static final int SIGNAL    = -1;

// 当前线程在condition队列中
static final int CONDITION = -2;

// 用于将唤醒后继线程传递下去,这个状态的引入是为了完善和增强共享锁的唤醒机制.在一个节点成为头节点之前,是不会跃迁为此状态的
static final int PROPAGATE = -3;

// 0 表示无状态
        
Status field, taking on only the values: 
SIGNAL: 
        The successor of this node is (or will soon be) blocked (via park), so the current node must unpark its successor when it releases or cancels. 
        To avoid races, acquire methods must first indicate they need a signal, then retry the atomic acquire, and then, on failure, block. 
CANCELLED: 
        This node is cancelled due to timeout or interrupt. Nodes never leave this state. In particular, a thread with cancelled node never again blocks. 
CONDITION: 
        This node is currently on a condition queue. 
        It will not be used as a sync queue node until transferred, at which time the status will be set to 0. (Use of this value here has nothing to do with the other uses of the field, but simplifies mechanics.) 
PROPAGATE: 
        A releaseShared should be propagated to other nodes. This is set (for head node only) in doReleaseShared to ensure propagation continues, even if other operations have since intervened. 
0: 
        None of the above The values are arranged numerically to simplify use. 

Non-negative values mean that a node doesn't need to signal. So, most code doesn't need to check for particular values, just for sign. 
The field is initialized to 0 for normal sync nodes, and CONDITION for condition nodes. 
It is modified using CAS (or when possible, unconditional volatile writes).
```