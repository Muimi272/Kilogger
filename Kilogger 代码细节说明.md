# Kilogger 代码细节说明

## 1. 总体架构

项目由两个核心类组成：

- `Kitimer`：时间字符串生成与秒级缓存。
- `Kilogger`：异步日志队列、日志线程、文件落盘与关闭流程。

---

## 2. `Kitimer` 说明

### 成员

- `private static final DateTimeFormatter FORMATTER`：时间格式化器（`yyyy-MM-dd HH:mm:ss`）。
- `private static volatile long lastCachedSecond`：上次缓存对应的秒级时间戳。
- `private static volatile String cachedTimeString`：上次缓存的格式化时间字符串。

### 方法

- `public static String getCurrentTime()`：获取当前时间字符串。

  该方法采用秒级缓存策略：如果当前调用仍处于同一秒，则直接返回缓存，避免重复格式化时间字符串，从而降低高并发下的时间格式化开销。

- `private static String updateCache(long currentSecond, long currentMillis)`：更新缓存内容。

  该方法是 `getCurrentTime()` 的辅助方法，用于写入本秒对应的格式化字符串并返回。

---

## 3. `Kilogger` 说明

### 成员

- `private static final int QUEUE_CAPACITY`：日志队列容量上限（`1024`）。
- `public static final String[] TYPES`：内置日志级别（`INFO/WARN/ERROR`）。
- `private static String LOG_FILE_PATH`：当前日志文件路径（默认 `log.txt`）。
- `private static BlockingQueue<String> logQueue`：日志缓冲队列。
- `private static Thread logThread`：后台日志线程。
- `private static volatile boolean isRunning`：日志线程运行标志。

### 3.1 `start()`

`start()` 负责初始化运行状态、准备日志文件并创建后台线程：

1. 当 `logThread == null` 时创建 `LinkedBlockingQueue`。
2. 设置 `isRunning = true`。
3. 根据 `LOG_FILE_PATH` 准备日志文件（不存在则创建）。
4. 创建并启动守护线程 `Kilogger-Thread`，持续从队列取日志并写入文件。
5. 设置 JVM 默认未捕获异常处理器，将异常转成日志。

关键实现点：

- `BlockingQueue<String> currentQueue = logQueue;`

  这是把当前队列引用固定到局部变量，避免线程闭包直接依赖可变静态字段。主要价值在于并发语义更稳妥、可维护性更好，而不是性能优化。

- `currentQueue.poll(100, TimeUnit.MILLISECONDS)`

  使用超时轮询而非 `take()`，可以让线程定期检查 `isRunning`，在关闭阶段更快退出。

- 内层 `catch (IOException e)`

  处理循环中的写入失败（`write/newLine/flush`）。

- 内层 `catch (InterruptedException e)`

  只会在阻塞等待（`poll`）被中断时触发，不是“队列被结束”。当前实现会恢复中断标记，并在 `!isRunning` 时退出循环。

- 外层 `catch (IOException e)`（线程内部）

  主要处理 `newBufferedWriter(...)` 打开失败或资源关闭失败。这里抛出的 `ExceptionInInitializerError` 发生在子线程中，语义上并非“类初始化失败”，只是使该日志线程异常终止。

### 3.2 `shutdown()`

`shutdown()` 的目标是尽量优雅停机：

1. 设置 `isRunning = false`，通知日志线程停止。
2. 若日志线程存在，则在必要时 `join()` 等待其结束。
3. 追加写入“日志线程已关闭”标记。

说明：

- `join()` 可能抛 `InterruptedException`，当前实现会恢复中断标记。
- 如果文件不可写，关闭标记可能写入失败。

### 3.3 `setLogFile(String logFile)`

执行顺序为：

1. 先调用 `info(...)` 记录“切换文件”日志（写入旧队列/旧文件）。
2. 调用 `shutdown()` 停止当前日志线程。
3. 更新 `LOG_FILE_PATH`。
4. 再调用 `start()` 使用新文件重启线程。

如果过程抛异常，方法返回 `false`。

### 3.4 其他 API

- `info/warn/error/log`：将字符串拼装后入队；若调用线程在 `put` 期间被中断，返回 `false`。
- `awake()`：空方法，仅用于触发类的主动使用，从而确保静态初始化提前发生。
- `getStackTrace(Throwable t)`：把异常堆栈转换为字符串，便于日志记录。

---

## 4. 静态代码块与生命周期

```java
static {
    start();
    Runtime.getRuntime().addShutdownHook(new Thread(Kilogger::shutdown));
}
```

说明：

- 类加载时会自动启动日志线程。
- 注册 `ShutdownHook` 后，JVM 正常退出路径（如 `main` 结束、`System.exit`）会尝试执行 `shutdown()`。
- `logThread` 是守护线程；如果没有 `ShutdownHook`，JVM 可能在仅剩守护线程时直接退出，导致队列未完全落盘。
- `ShutdownHook` 也并非绝对保证：如强制杀进程、JVM 崩溃、系统断电等场景可能来不及执行。

---

## 5. 边界与注意事项

- `Thread.setDefaultUncaughtExceptionHandler(...)` 是全局设置，会影响进程内其他线程的默认异常处理行为。
- 当前实现每条日志都 `flush()`，可靠性高但吞吐受 I/O 影响较大。
- 若日志生产速度长期高于消费速度，`put()` 可能阻塞业务线程（队列容量固定）。
- 文档中的行为描述以当前 `src/main/java/com/muimi/Kilogger.java` 实现为准。

