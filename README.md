# Kilogger

一个轻量级的 Java 异步日志工具，支持多线程写入、日志级别区分、运行时切换日志文件，以及程序退出前的日志队列清空。

[![](https://jitpack.io/v/Muimi272/Kilogger.svg)](https://jitpack.io/#Muimi272/Kilogger)

---

## 中文文档

### 功能特性

- 异步写日志：使用后台线程消费队列，减少业务线程阻塞。
- 多日志级别：内置 `INFO`、`WARN`、`ERROR`。
- 自定义日志类型：支持 `log(message, type)`。
- 动态切换日志文件：`setLogFile("new.txt")`。
- 安全关闭：`shutdown()` 会等待队列消费完成。
- 统一时间戳：格式为 `[yyyy-MM-dd HH:mm:ss]`。

### 项目结构

```text
Kilogger/
|- pom.xml
|- src/
|  \- main/
|     \- java/
|        |- Main.java
|        \- com/muimi/
|           |- Kilogger.java
|           \- Kitimer.java
|- LICENSE
\- README.md
```

### 导入项目

### Gradle

1. 在根 `settings.gradle`（或 `settings.gradle.kts`）中添加 JitPack 仓库：

```groovy
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}
```

2. 在模块 `build.gradle` 中添加依赖：

```groovy
dependencies {
	implementation 'com.github.Muimi272:Kilogger:v1.0.0'
}
```

### Maven

1. 在 `pom.xml` 中添加 JitPack 仓库：

```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```

2. 添加依赖：

```xml
<dependency>
	<groupId>com.github.Muimi272</groupId>
	<artifactId>Kilogger</artifactId>
	<version>v1.0.0</version>
</dependency>
```

### 运行源代码

1) 编译

```powershell
mvn clean compile
```

2) 打包

```powershell
mvn clean package
```

3) 运行示例

```powershell
java -cp target\classes Main
```

运行后会生成日志文件（默认 `log.txt`，切换后如 `new.txt`）。

### 使用示例

```java
import com.muimi.Kilogger;

public class Demo {
	public static void main(String[] args) {
		Kilogger.awake();
		Kilogger.info("Service started");
		Kilogger.warn("Memory usage is high");
		Kilogger.error("Request failed");
		Kilogger.log("custom message", "debug");
		Kilogger.setLogFile("runtime.log");
		Kilogger.info("Now writing to runtime.log");
	}
}
```

### API 概览

- `void awake()`：唤醒日志线程初始化，建议在程序入口调用一次。
- `boolean info(String message)`：写入 INFO 日志。
- `boolean warn(String message)`：写入 WARN 日志。
- `boolean error(String message)`：写入 ERROR 日志。
- `boolean log(String message, String type)`：按自定义类型写日志。
- `boolean log(String message)`：等价于 `info(message)`。
- `boolean setLogFile(String logFile)`：切换输出文件并重启日志线程。
- `void shutdown()`：关闭日志线程并尝试写完剩余日志。该方法也已注册到 JVM `ShutdownHook`。

> 返回值为 `false` 通常表示线程被中断或切换失败。

### 线程模型说明

- 内部使用 `LinkedBlockingQueue`（容量 `1000`）缓存日志。
- 业务线程调用日志方法时将消息入队。
- 后台守护线程从队列取出并追加写入文件。
- 通过 `shutdown()` 停止线程并等待队列清空。

### 注意事项

- 建议在程序入口处调用 `Kilogger.awake()`，以确保日志线程提前初始化。
- `Kilogger` 在类加载时会设置默认未捕获异常处理器，异常会写入日志。
- 默认日志文件为 `log.txt`，运行示例中会切换到 `new.txt`。

### 许可证

本项目采用 MIT License，详见 `LICENSE` 文件。

---

## English Documentation

### Features

- Asynchronous logging with a background consumer thread to reduce blocking in business threads.
- Built-in log levels: `INFO`, `WARN`, and `ERROR`.
- Custom log types via `log(message, type)`.
- Runtime log file switching using `setLogFile("new.txt")`.
- Graceful shutdown: `shutdown()` waits for queued logs to be flushed.
- Unified timestamp format: `[yyyy-MM-dd HH:mm:ss]`.

### Project Structure

```text
Kilogger/
|- pom.xml
|- src/
|  \- main/
|     \- java/
|        |- Main.java
|        \- com/muimi/
|           |- Kilogger.java
|           \- Kitimer.java
|- LICENSE
\- README.md
```

### Installation

#### Gradle

1. Add JitPack to your root `settings.gradle` (or `settings.gradle.kts`):

```groovy
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}
```

2. Add dependency in your module `build.gradle`:

```groovy
dependencies {
	implementation 'com.github.Muimi272:Kilogger:v1.0.0'
}
```

#### Maven

1. Add JitPack repository in your `pom.xml`:

```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```

2. Add dependency:

```xml
<dependency>
	<groupId>com.github.Muimi272</groupId>
	<artifactId>Kilogger</artifactId>
	<version>v1.0.0</version>
</dependency>
```

### Run from Source

1) Compile

```powershell
mvn clean compile
```

2) Package

```powershell
mvn clean package
```

3) Run demo

```powershell
java -cp target\classes Main
```

After running, log files are generated (`log.txt` by default, and `new.txt` after file switching).

### Usage Example

```java
import com.muimi.Kilogger;

public class Demo {
	public static void main(String[] args) {
		Kilogger.awake();
		Kilogger.info("Service started");
		Kilogger.warn("Memory usage is high");
		Kilogger.error("Request failed");
		Kilogger.log("custom message", "debug");
		Kilogger.setLogFile("runtime.log");
		Kilogger.info("Now writing to runtime.log");
	}
}
```

### API Overview

- `void awake()`: Wakes logger initialization; recommended once at application startup.
- `boolean info(String message)`: Writes an INFO log.
- `boolean warn(String message)`: Writes a WARN log.
- `boolean error(String message)`: Writes an ERROR log.
- `boolean log(String message, String type)`: Writes a log with a custom type.
- `boolean log(String message)`: Equivalent to `info(message)`.
- `boolean setLogFile(String logFile)`: Switches output file and restarts the logging thread.
- `void shutdown()`: Stops logger thread and flushes remaining logs when possible. Also registered in JVM `ShutdownHook`.

> `false` usually indicates interruption or switching failure.

### Threading Model

- Uses a `LinkedBlockingQueue` (capacity `1000`) for buffering.
- Application threads enqueue messages through log APIs.
- A daemon thread consumes queue entries and appends them to file.
- `shutdown()` stops the thread and waits for queue draining.

### Notes

- Call `Kilogger.awake()` at startup for early logger initialization.
- `Kilogger` sets a default uncaught exception handler during class loading.
- Default log file is `log.txt`; the demo switches to `new.txt`.

### License

This project is licensed under the MIT License. See `LICENSE` for details.
