# Kilogger

一个轻量级的 Java 异步日志工具，支持多线程写入、日志级别区分、运行时切换日志文件，以及程序退出前的日志队列清空。

## 功能特性

- 异步写日志：使用后台线程消费队列，减少业务线程阻塞。
- 多日志级别：内置 `INFO`、`WARN`、`ERROR`。
- 自定义日志类型：支持 `log(message, type)`。
- 动态切换日志文件：`setLogFile("new.txt")`。
- 安全关闭：`shutdown()` 会尽量等待队列消费完成。
- 统一时间戳：格式为 `[yyyy-MM-dd HH:mm:ss]`。

## 项目结构

```text
Kilogger/
├─ src/
│  ├─ Main.java                 # 示例入口（演示多线程日志）
│  └─ com/muimi/
│     ├─ Kilogger.java          # 核心日志实现
│     └─ Kitimer.java           # 时间戳缓存工具
```

## 快速开始

### 1) 编译

在项目根目录执行：

```powershell
javac -encoding UTF-8 -d out src\com\muimi\*.java src\Main.java
```

### 2) 运行示例

```powershell
java -cp out Main
```

运行后会生成日志文件（默认 `log.txt`，切换后如 `new.txt`）。

## 使用示例

```java
import main.java.com.muimi.Kilogger;

public class Demo {
    public static void main(String[] args) {
        Kilogger.info("Service started");
        Kilogger.warn("Memory usage is high");
        Kilogger.error("Request failed");

        Kilogger.log("custom message", "debug");

        Kilogger.setLogFile("runtime.log");
        Kilogger.info("Now writing to runtime.log");

        Kilogger.shutdown();
    }
}
```

## API 概览

- `boolean info(String message)`：写入 INFO 日志。
- `boolean warn(String message)`：写入 WARN 日志。
- `boolean error(String message)`：写入 ERROR 日志。
- `boolean log(String message, String type)`：按自定义类型写日志。
- `boolean log(String message)`：等价于 `info(message)`。
- `boolean setLogFile(String logFile)`：切换输出文件并重启日志线程。
- `void shutdown()`：关闭日志线程并尝试写完剩余日志。

> 返回值为 `false` 通常表示线程被中断或切换失败。

## 线程模型说明

- 内部使用 `LinkedBlockingQueue`（容量 `1000`）缓存日志。
- 业务线程调用日志方法时将消息入队。
- 后台守护线程从队列取出并追加写入文件。
- 通过 `shutdown()` 停止线程并等待队列清空。

## 注意事项

- 请在程序退出前调用 `Kilogger.shutdown()`，避免日志丢失。
- `Kilogger` 在类加载时会设置全局默认未捕获异常处理器，异常会写入日志。
- 默认日志文件为 `log.txt`，运行示例中会切换到 `new.txt`。

## GitHub 提交建议（基于当前 .gitignore）

当前 `.gitignore` 已忽略 IDE 和构建产物（如 `out/`、`.idea/`、`*.iml`），这是合理的。

另外两条规则可能影响仓库展示：

- `**/Main.java`：会忽略示例入口文件。
- `.gitignore`：会忽略 `.gitignore` 本身。

如果你希望 GitHub 仓库包含示例入口和忽略规则文件，建议移除上述两条后再提交。

## License

本项目采用 MIT License，详见 `LICENSE` 文件。
