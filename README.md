# Android总结
## 线程池
### 线程池的优点
* 重用线程池里面的线程，避免线程的创建和销毁带来的性能开销。
* 能有效控制线程池的最大并发数，避免大量线程之间因为相互抢占系统资源而导致的阻塞。
### 线程池ThreadPoolExecutor构造参数解析
* corePoolSize 线程池核心线程数,默认情况下,核心线程会在线程池中一直存在,即使他们处于闲置状态。
如果将allowCoreThreadTimeOut设置为true,那么闲置的核心线程会有超时策略.
* maximumPoolSize 线程池最大容纳线程数,当活动线程达到这个数值后,后续的新任务将会被阻塞.
* keepAliveTime 非核心线程闲置超时时间,超过这个时间非核心线程就会被回收,当allowCoreThreadTimeOut
设置为true时,同样也作用于核心线程。
* unit 超时时间的时间单位
* workQueue 任务队列,通过线程池的execute方法提交的Runnable对象会存储在这个队列中。
* threadFactory 线程工厂,为线程池创建新线程的功能。
* ExecutionHandler 当任务队列已满,默认情况下回直接抛出RejectedExecutionException。
### 线程池ThreadPoolExecutor任务执行顺序
* 1.如果线程池中的线程数量未达到核心线程的数量,那么直接启动一个核心线程来执行任务
* 2.如果线程池中的线程数已经达到或者超过核心线程的数量,那么任务会被插入到任务队列中排队
* 3.如果(2)插入队列失败,往往是由于任务队列已满,这个时候如果线程数量没有达到线程池规定的最大值,那么会启动一个
非核心线程来执行。如果任务队列未满,任务由核心线程执行。
* 4.如果(3)中线程池数据已经达到线程池规定的最大值,那么就回拒绝执行此任务,ThreadPoolExecutor会调用
rejectedExecution方法来通知调用者。

