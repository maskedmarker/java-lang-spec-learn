# monitor锁

```text
synchronized的实现是基于monitor对象(也称为监视器锁或内部锁)

当你使用 synchronized 修饰代码块或方法时，编译器会在编译后的字节码中插入特定的指令来实现同步。

对于同步代码块：
monitorenter： 指令被插入到同步代码块的开始位置。线程试图获取对象的 Monitor 锁。
monitorexit： 指令被插入到同步代码块的结束位置（包括正常退出和异常退出），用于释放 Monitor 锁。
```

## EntryList/WaitSet

```text
JLS中要求monitor拥有2个相关容器:
EntryList: 等待锁的主要队列,顺序由JVM策略决定(FIFO/FILO).当线程竞争monitor锁失败时,将自身包装成ObjectWaiter节点然后入队该队列.
WaitSet: 一个集合.当一个已经获得锁的线程调用wait()时，当前线程会释放已经持有的Monitor锁，然后自身包装成ObjectWaiter节点,加入到WaitSet中

在HotSpot的JVM实现中,
EntryList/WaitSet都是通过一个双向链表实现的.所以称呼EntryList为入口队列,WaitSet为等待队列(或条件队列)
还有一个用单向链表实现的名叫cxq队列(Contention Queue 竞争队列),用于辅助实现JLS要求EntryList/WaitSet的功能.
(底层中的队列都采用的是lock-free queue)

cxq队列的入队/出队
当一个线程请求锁但发现锁已经被其他线程持有时，该线程会被封装成一个ObjectWaiter对象，并被加入到 cxq 的队尾。
当持有锁的线程释放锁后，会从 cxq 的队头取出一个线程，将其移动到 EntryList 中，以便该线程有机会尝试获取锁.
(备注: 在锁升级为重量级锁前,使用cxq)

线程唤醒
当持有锁的线程释放锁时，会根据一定的策略从cxq或EntryList中选择线程进行唤醒。如果EntryList为空，而cxq不为空，那么会将cxq中的线程转移到EntryList中，然后从EntryList的头部唤醒线程尝试获取锁。
而WaitSet中的线程在被唤醒后，也会根据情况被放入cxq或EntryList中，等待重新竞争锁。
```

```text
获取monitor锁失败 -> 线程进入entry-queue(入口队列),然后挂起自己
wait() -> 线程进入 wait-queue(等待队列),然后挂起自己
notify/notifyAll -> 线程从 wait-queue重新放入entry-queue
interrupt -> 线程被唤醒,然后去抢占锁如果失败继续挂起自己,如果成功将自己节点从队列移除,

内置锁(monitor)只提供了排他性,防止其他线程并发执行;至于当前环境状态是否满足业务需要,需要用户自己来判断.
```



## 例子

### notify例子

```text
// 线程A（锁持有者）
synchronized (obj) {
    // 做一些工作...
    obj.notify();  // 将线程B从WaitSet移到cxq/EntryList
    // 继续做其他工作...
} // 释放锁,并从cxq/EntryList队列中选中线程B节点,然后唤醒线程B


// 线程B（等待者）  
synchronized (obj) {
    while (!condition) {
        obj.wait();  // 进入WaitSet,释放锁,挂起自己;被唤醒重新争抢到锁,从cxq/EntryList队列移除对应节点,退出wait方法
    }
    // 满足condition后跳出while循环(否则继续执行wait),执行后续工作
}
```

### interrupt例子

```text
// 线程A（锁持有者）
synchronized (obj) {
    // 做一些工作...
    threadB.interrupt();  // 将线程B从挂起唤醒
    // 继续做其他工作...
} // 这里释放锁时，才会真正唤醒线程B


// 线程B（等待者）  
synchronized (obj) {
    while (!condition) {
        obj.wait();  // 进入WaitSet,释放锁,挂起自己;被唤醒重新争抢到锁,从WaitSet队列移除对应节点,退出wait方法
    }
    // 满足condition后,结束while循环,执行后续工作
}
```

## 底层实现

### monitorenter底层实现

```text
void ObjectMonitor::enter(TRAPS) {
  Thread * const Self = THREAD;
  
  // 尝试快速获取锁
  if (TryLock(Self) > 0) {
    return;  // 快速获取成功
  }
  
  // 尝试自旋获取锁
  if (TrySpin(Self) > 0) {
    return;  // 自旋获取成功
  }
  
  // 慢速路径：进入重量级锁竞争
  for (;;) {
    ObjectWaiter node(Self);
    Self->_ParkEvent->reset();
    node._prev   = (ObjectWaiter *) 0xBAD;
    node.TState  = ObjectWaiter::TS_CXQ;
    
    // 将节点推入cxq队列的头部
    ObjectWaiter * nxt;
    for (;;) {
      node._next = nxt = _cxq;
      if (Atomic::cmpxchg_ptr (&node, &_cxq, nxt) == nxt) {
        break;  // CAS成功，节点已加入cxq
      }
    }
    
    // 再次尝试获取锁
    if (TryLock(Self) > 0) {
      return;
    }
    
    // 最终，挂起线程等待唤醒
    Self->_ParkEvent->park();
    
    // 被唤醒后，再次尝试获取锁
    if (TryLock(Self) > 0) {
      return;
    }
  }
}


int ObjectMonitor::TryLock(Thread * Self) {
  void * own = _owner;
  if (own != NULL) return 0;  // 已经有owner，失败
  
  // 尝试CAS设置owner为当前线程
  if (Atomic::cmpxchg_ptr (Self, &_owner, NULL) == NULL) {
    return 1;  // CAS成功，获取锁
  }
  return 0;  // CAS失败
}
```


###  monitorexit底层实现

```text
void ObjectMonitor::exit(bool not_suspended, TRAPS) {
  // ... 前面的逻辑省略 ...
  
  // 关键：唤醒后继线程
  if (_EntryList != NULL || _cxq != NULL) {
    // 需要唤醒等待的线程
    TEVENT(Inflated exit - Reenter);
    
    // 调用退出协议，这会唤醒等待的线程
    ExitEpilog(Self, not_suspended ? &_not_suspended : NULL);
    return;
  }
  
  // ... 其他情况处理 ...
}

// 退出协议：真正唤醒线程的地方
void ObjectMonitor::ExitEpilog(Thread * Self, ParkEvent * Trigger) {
  // 从竞争队列中选取一个线程来唤醒
  ObjectWaiter * Wakee = NULL;
  
  // 策略：优先从EntryList唤醒，如果没有则从cxq迁移
  if (_EntryList != NULL) {
    Wakee = _EntryList;
    _EntryList = Wakee->_next;
    if (_EntryList != NULL) {
      _EntryList->_prev = NULL;
    }
  } else {
    // 从cxq中迁移线程到EntryList，然后唤醒
    if (_cxq != NULL) {
      Wakee = DequeueWaiterFromCxq();
    }
  }
  
  if (Wakee != NULL) {
    // 真正唤醒线程
    ParkEvent * ev = Wakee->_event;
    Wakee->TState = ObjectWaiter::TS_RUN;
    OrderAccess::fence();
    
    // 关键调用：唤醒线程
    ev->unpark();
    
    TEVENT(exit - unpark);
  }
  
  // 如果有触发事件，也唤醒它
  if (Trigger != NULL) {
    Trigger->unpark();
  }
}

// ------------------------------
void ParkEvent::unpark() {
  if (Atomic::xchg(1, &_counter) == 0) {
    // 使用pthread_cond_signal唤醒等待的线程
    int status = pthread_cond_signal(_cond);
    assert_status(status == 0, status, "cond_signal");
    
    TEVENT(park - unpark);
  }
}
```

### wait底层实现

注意:
1. wait会导致当前线程入队WaitSet然后挂起自己
2. 

```text
int ObjectSynchronizer::wait(Handle obj, jlong millis, TRAPS) {
  if (UseBiasedLocking) {
    // 如果启用了偏向锁，需要先撤销偏向
    BiasedLocking::revoke_and_rebias(obj, false, THREAD);
    assert(!obj->mark()->has_bias_pattern(), "biases should be revoked by now");
  }
  if (millis < 0) {
    TEVENT(wait - throw IAX);
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "timeout value is negative");
  }
  // 关键调用：通过ObjectMonitor的wait方法实现
  ObjectMonitor* monitor = ObjectSynchronizer::inflate(THREAD, obj());
  monitor->wait(millis, true, THREAD);
  return dtrace_waited_probe(monitor, obj, THREAD);
}

// 这里的interruptible为true
void ObjectMonitor::wait(jlong millis, bool interruptible, TRAPS) {
  Thread * const Self = THREAD;
  
  // 检查中断状态
  if (Thread::is_interrupted(Self, true) && interruptible) {
    throw_interruptedException = true;
    return;
  }
  
  // 创建ObjectWaiter节点，用于放入WaitSet
  ObjectWaiter node(Self);
  node.TState = ObjectWaiter::TS_WAIT;
  Self->_ParkEvent->reset();
  OrderAccess::fence();

  // 将节点加入WaitSet队列
  AddWaiter(&node);  // 关键步骤1

  // 记录等待开始时间，用于超时控制
  jlong prevtime = os::javaTimeNanos();
  jlong limit = prevtime + millis * 1000000;

  // 完全退出管程（释放锁）
  exit(true, Self); // 关键步骤2：释放所有递归计数

  int ret = OS_OK;
  bool is_timed = (millis != 0);  // 是否设置了超时

  for (;;) {
    if (interruptible && Thread::is_interrupted(Self, true)) {
      ret = OS_INTRPT;  // 被中断
      break;
    }

    if (is_timed) {
      jlong newtime = os::javaTimeNanos();
      if (newtime - prevtime < 0) newtime = prevtime; // 时间回退处理
      if (newtime >= limit) {
        ret = OS_TIMEOUT;  // 超时
        break;
      }
      prevtime = newtime;
    }

    // 关键步骤3：线程挂起（等待通知）
    Self->_ParkEvent->park(is_timed ? (jlong)(limit - prevtime) : 0);
    
    // 检查是否被notify/notifyAll唤醒
    if (node._notified != 0) {
      ret = OS_OK;  // 被正常唤醒
      break;
    }
  }

  // 被唤醒后，需要重新获取锁
  enter(THREAD);  // 关键步骤4：重新竞争锁

  // 获得锁后,从WaitSet中移除节点
  if (node.TState == ObjectWaiter::TS_WAIT) {
    DequeueSpecificWaiter(&node);
  }
}

// 将ObjectWaiter节点添加到WaitSet的尾部（FIFO策略）
inline void ObjectMonitor::AddWaiter(ObjectWaiter* node) {
  assert(node != NULL, "should not add null node");
  node->_next = NULL;
  
  // 如果WaitSet不为空，将新节点添加到尾部
  if (_WaitSet == NULL) {
    _WaitSet = node;
    node->_prev = NULL;
  } else {
    ObjectWaiter* head = _WaitSet;
    while (head->_next != NULL) {
      head = head->_next;
    }
    head->_next = node;
    node->_prev = head;
  }
}
```


### notify底层实现

注意: 
notify()并不直接唤醒线程，而是将线程从WaitSet 迁移到竞争队列(或入口队列)，真正的唤醒发生在锁释放时执行ObjectMonitor::exit
这样允许锁持有者在调用notify()后继续执行一些工作,被唤醒的线程在重新获取锁时能看到锁持有者完成的所有修改.


```text
void ObjectSynchronizer::notify(Handle obj, TRAPS) {
  if (UseBiasedLocking) {
    // 如果启用了偏向锁，需要先撤销偏向
    BiasedLocking::revoke_and_rebias(obj, false, THREAD);
    assert(!obj->mark()->has_bias_pattern(), "biases should be revoked by now");
  }
  
  // 获取对象的ObjectMonitor（必须是重量级锁）
  markOop mark = obj->mark();
  if (mark->has_monitor()) {
    ObjectMonitor* monitor = mark->monitor();
    // 关键调用：通过ObjectMonitor的notify方法实现
    monitor->notify(THREAD);
  }
}

void ObjectMonitor::notify(TRAPS) {
  CHECK_OWNER();  // 检查当前线程是否持有锁
  
  if (_WaitSet == NULL) {
    // WaitSet为空，没有等待的线程，直接返回
    TEVENT(empty - notify);
    return;
  }
  
  DTRACE_MONITOR_PROBE(notify, this, object(), THREAD);
  
  int Policy = Knob_MoveNotifyee;  // 获取通知策略配置
  
  // 关键操作：从WaitSet中移除一个线程
  Thread::SpinAcquire(&_WaitSetLock, "WaitSet - notify");
  ObjectWaiter* iterator = DequeueWaiter();  // 从WaitSet头部取一个节点
  if (iterator != NULL) {
    TEVENT(notify - extract one);
  }
  Thread::SpinRelease(&_WaitSetLock);
  
  if (iterator != NULL) {
    // 根据策略处理被通知的线程
    DequeueSpecificWaiter(iterator);  // 从WaitSet中完全移除
    
    ObjectWaiter::TStates v = iterator->TState;
    
    if (v == ObjectWaiter::TS_WAIT) {
      // 正常等待状态，准备迁移到竞争队列
      iterator->TState = ObjectWaiter::TS_ENTER;
      
      // 根据策略决定将线程放入哪个队列
      if (Policy == 0) { 
        // 策略0:  prepend to cxq
        if (_cxq == NULL) {
          iterator->_next = iterator->_prev = NULL;
          _cxq = iterator;
        } else {
          iterator->_next = _cxq;
          iterator->_prev = NULL;
          _cxq->_prev = iterator;
          _cxq = iterator;
        }
      } else if (Policy == 1) { 
        // 策略1: append to cxq
        if (_cxq == NULL) {
          iterator->_next = iterator->_prev = NULL;
          _cxq = iterator;
        } else {
          ObjectWaiter* tail;
          for (tail = _cxq; tail->_next != NULL; tail = tail->_next);
          tail->_next = iterator;
          iterator->_prev = tail;
          iterator->_next = NULL;
        }
      } else if (Policy == 2) { 
        // 策略2: prepend to EntryList
        if (_EntryList == NULL) {
          iterator->_next = iterator->_prev = NULL;
          _EntryList = iterator;
        } else {
          iterator->_next = _EntryList;
          iterator->_prev = NULL;
          _EntryList->_prev = iterator;
          _EntryList = iterator;
        }
      } else if (Policy == 3) {
        // 策略3: append to EntryList  
        if (_EntryList == NULL) {
          iterator->_next = iterator->_prev = NULL;
          _EntryList = iterator;
        } else {
          ObjectWaiter* tail;
          for (tail = _EntryList; tail->_next != NULL; tail = tail->_next);
          tail->_next = iterator;
          iterator->_prev = tail;
          iterator->_next = NULL;
        }
      } else {
        // 默认策略: 尝试直接唤醒
        ParkEvent* ev = iterator->_event;
        iterator->TState = ObjectWaiter::TS_RUN;
        OrderAccess::fence();
        ev->unpark();  // 直接唤醒线程
      }
      
      if (Policy < 4) {
        iterator->wait_reenter_begin(this);
      }
    } else {
      // 线程已经被取消或其他状态
      TEVENT(notify - unneeded wakeup);
    }
  }
}


// 从WaitSet队列头部移除并返回一个ObjectWaiter节点
inline ObjectWaiter* ObjectMonitor::DequeueWaiter() {
  ObjectWaiter* waiter = _WaitSet;
  if (waiter != NULL) {
    DequeueSpecificWaiter(waiter);  // 从链表中移除
  }
  return waiter;
}

// 从WaitSet双向链表中移除特定节点
inline void ObjectMonitor::DequeueSpecificWaiter(ObjectWaiter* node) {
  assert(node != NULL, "should not dequeue null node");
  
  if (node->_prev != NULL) {
    node->_prev->_next = node->_next;
  } else {
    _WaitSet = node->_next;  // 更新头指针
  }
  
  if (node->_next != NULL) {
    node->_next->_prev = node->_prev;
  }
  
  // 清空节点的前后指针
  node->_next = NULL;
  node->_prev = NULL;
}

void os::PlatformEvent::unpark() {
  if (Atomic::xchg(1, &_counter) == 0) {
    // 使用pthread_cond_signal唤醒等待的线程
    status = pthread_cond_signal(_cond);
    assert_status(status == 0, status, "cond_signal");
  }
}
```

### interrupt底层实现
```text
void Thread::interrupt(Thread* thread) {
  debug_only(check_for_dangling_thread_pointer(thread);)
  
  // 设置中断标志
  os::interrupt(thread);
}

// --------------------------------------------------------------------

// 操作系统抽象层的中断实现
void os::interrupt(Thread* thread) {
  // 检查线程状态
  if (!thread->thread_state()->is_running()) {
    return;
  }
  
  // 获取线程的OSThread
  OSThread* osthread = thread->osthread();
  if (osthread == NULL) {
    return;
  }
  
  // 设置中断状态
  osthread->set_interrupted(true);
  
  // 对于阻塞在对象监视器上的线程
  if (thread->is_Java_thread()) {
    JavaThread* jt = (JavaThread*)thread;
    
    // 检查是否在对象监视器上等待
    if (jt->thread_state() == _thread_blocked) {
      // 对于synchronized阻塞，需要特殊处理
      jt->pd_unblock();
    }
  }
  
  // 调用平台相关的中断实现
  pd_interrupt(thread);
  
  // 对于使用ParkEvent挂起的线程
  ParkEvent * ev = thread->_ParkEvent ;
  if (ev != NULL) {
    ev->unpark() ;
  }
}

// --------------------------------------------------------------------

// Linux平台相关实现
void os::Linux::pd_interrupt(Thread* thread) {
  OSThread* osthread = thread->osthread();
  
  if (!osthread->interrupted()) {
    // 设置中断标志
    osthread->set_interrupted(true);
    
    // 获取目标线程ID
    pthread_t tid = osthread->pthread_id();
    
    // 发送信号来中断阻塞的系统调用
    int ret = pthread_kill(tid, SR_signum);
    
    // 清除线程的挂起状态（如果有）
    if (thread->is_Java_thread()) {
      JavaThread* jt = (JavaThread*)thread;
      jt->pd_unblock();
    }
    
    // 唤醒使用ParkEvent挂起的线程
    ParkEvent * ev = thread->_ParkEvent ;
    if (ev != NULL) {
      ev->unpark();
    }
  }
}

// --------------------------------------------------------------------

// 信号处理程序，处理中断信号
static void jdk_signal_handler(int sig, siginfo_t* info, void* uc) {
  // 检查是否是中断信号
  if (sig == SR_signum) {
    // SR_signum 通常是 SIGUSR1 或 SIGRTMIN + 1
    
    // 获取当前线程
    JavaThread* thread = JavaThread::current_or_null_safe();
    if (thread != NULL) {
      OSThread* osthread = thread->osthread();
      
      // 设置中断状态
      osthread->set_interrupted(true);
      
      // 清除挂起状态
      thread->pd_unblock();
      
      // 对于不同的阻塞状态，执行不同的唤醒逻辑
      if (thread->thread_state() == _thread_in_native) {
        // 在native代码中，设置中断状态即可
      } else if (thread->thread_state() == _thread_in_vm) {
        // 在VM代码中
      } else if (thread->thread_state() == _thread_blocked) {
        // 在阻塞状态，需要唤醒
        thread->pd_unblock();
      }
    }
    return;  // 不终止线程
  }
  
  // 其他信号的处理...
}
```

### Atomic::cmpxchg

```text
cmpxchgl 汇编指令是整个 Atomic::cmpxchg 方法的核心

cmpxchgl 指令是包含在 x86 架构及 IA-64 架构中的一个原子条件指令，
它会首先比较dest指针指向的内存值是否和 compare_value 的值相等，如果相等，则双向交换 dest 与 exchange_value，否则就单方面地将 dest 指向的内存值交给exchange_value。
这条指令完成了整个 CAS 操作，因此它也被称为 CAS 指令。


cas(address, expectedValue, newValue)执行原子操作时,如果该内存处的值为expectedValue,则内存处的值更新为newValue;如果内存处的值不是expectedValue,则内存处的值不做更新.cas总是返回当前内存处的值.

cmpxchg等价于
boolean cmpxchg(newValue, *address, expectedValue){
   return cas(address, expectedValue, newValue) == expectedValue;
}
```