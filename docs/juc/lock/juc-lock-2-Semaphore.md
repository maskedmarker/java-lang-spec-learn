# juc-AQS-Semaphore


```text
// Semaphore不支持Lock接口,也就不支持Condition
public class Semaphore implements java.io.Serializable {
    private final Sync sync;
}
```
