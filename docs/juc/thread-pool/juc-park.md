# juc-park

juc中提供的新的挂起线程的工具类:
java.util.concurrent.locks.LockSupport


不管是VirtualThread还是Thread类,在使用park时,
都是当前"(虚拟)线程"基于对某种条件的判断,需要先暂时挂起,自己主动调用LockSupport.park(),而非其他线程调用另一个线程;等待条件满足后,会被其他线程LockSupport.unpark().🎯🎯🎯🎯🎯🎯

注意: LockSupport.park()的语义是哪个线程调用当前方法,哪个线程就被挂起.所以不存在其他线程通过调用park让另一个线程挂起.

## park
```text
java.util.concurrent.locks.LockSupport.park(java.lang.Object)

Disables the current thread for thread scheduling purposes unless the permit is available.
If the permit is available then it is consumed and the call returns immediately; 
otherwise the current thread becomes disabled for thread scheduling purposes and lies dormant until one of three things happens:
    Some other thread invokes unpark with the current thread as the target; or
    Some other thread interrupts the current thread; or
    The call spuriously (that is, for no reason) returns.
This method does not report which of these caused the method to return. Callers should re-check the conditions which caused the thread to park in the first place. Callers may also determine, for example, the interrupt status of the thread upon return.

Params: 
    blocker – the synchronization object responsible for this thread parking (即调用park方法的caller)
    
退出park方法时,
有可能是被unpark了,
有可能是被interrupt了(因为没有抛出InterruptedException,就需要自己check interrupted status) 
甚至是无理由的return,
不管是哪种原因,都需要像monitor一样,将park放在while(condition_expression)中
```


## unpark

```text
java.util.concurrent.locks.LockSupport.unpark

Makes available the permit for the given thread, if it was not already available. If the thread was blocked on park then it will unblock. Otherwise, its next call to park is guaranteed not to block. This operation is not guaranteed to have any effect at all if the given thread has not been started.

Params:
    thread – the thread to unpark, or null, in which case this operation has no effect

```