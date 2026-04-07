


```text
AbortPolicy
CallerRunsPolicy

DiscardOldestPolicy
DiscardPolicy
```


```text
java.util.concurrent.ThreadPoolExecutor.setMaximumPoolSize(int maximumPoolSize)

Sets the maximum allowed number of threads. This overrides any value set in the constructor. 
If the new value is smaller than the current value, excess existing threads will be terminated when they next become idle.
```

```text
java.util.concurrent.ThreadPoolExecutor.allowCoreThreadTimeOut(boolean value)

Sets the policy governing whether core threads may time out and terminate if no tasks arrive within the keep-alive time, being replaced if needed when new tasks arrive. 
When false, core threads are never terminated due to lack of incoming tasks. When true, the same keep-alive policy applying to non-core threads applies also to core threads. 
To avoid continual thread replacement, the keep-alive time must be greater than zero when setting true. 
This method should in general be called before the pool is actively used.
```