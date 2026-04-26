package com.muimi;

import club.muimi.Kitimer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"CallToPrintStackTrace", "unused", "UnusedReturnValue"})
public class Kilogger {
    private static final int QUEUE_CAPACITY = 1024;
    public static final String[] TYPES = {"INFO", "WARN", "ERROR"};

    private static String LOG_FILE_PATH = "log.txt";

    private static BlockingQueue<String> logQueue;
    private static Thread logThread;

    private static volatile boolean isRunning = false;

    static {
        Kitimer.setFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()));
        start();
        Runtime.getRuntime().addShutdownHook(new Thread(Kilogger::shutdown));
    }

    /**
     * This method has an empty body.
     * Its purpose is to wake up the initialization of the logging thread and ensure that it is ready before being called, to avoid delays or thread safety issues when the logging method is called for the first time.
     */
    public static void awake() {
        // Empty Method
    }

    private static void start() {
        if (logThread == null) logQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        isRunning = true;
        try {
            Path logPath = Paths.get(LOG_FILE_PATH);
            if (!Files.exists(logPath)) Files.createFile(logPath);
            BlockingQueue<String> currentQueue = logQueue;
            logThread = new Thread(() -> {
                try (BufferedWriter bw = Files.newBufferedWriter(logPath, StandardOpenOption.APPEND)) {
                    bw.write(Kitimer.getCurrentTime() + " INFO: 日志线程已启动");
                    bw.newLine();
                    bw.flush();
                    while (isRunning || !currentQueue.isEmpty()) {
                        try {
                            String logEntry = currentQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (logEntry == null) continue;
                            bw.write(logEntry);
                            bw.newLine();
                            bw.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                            if (!isRunning) break;
                        }
                    }
                } catch (IOException e) {
                    throw new ExceptionInInitializerError("初始化日志文件失败: " + e.getMessage());
                }
            });
            logThread.setName("Kilogger-Thread");
            logThread.setDaemon(true);
            logThread.start();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Kilogger.error("线程 [" + thread.getName() + "] 抛出异常：\n" + getStackTrace(throwable) + "\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean setLogFile(String logFile) {
        info("更换输出文件：" + LOG_FILE_PATH + " -> " + logFile);
        try {
            shutdown();
            LOG_FILE_PATH = logFile;
            start();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean info(String message) {
        try {
            logQueue.put(Kitimer.getCurrentTime() + " " + TYPES[0] + ": " + message);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean warn(String message) {
        try {
            logQueue.put(Kitimer.getCurrentTime() + " " + TYPES[1] + ": " + message);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean error(String message) {
        try {
            logQueue.put(Kitimer.getCurrentTime() + " " + TYPES[2] + ": " + message);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean log(String message, String type) {
        try {
            logQueue.put(Kitimer.getCurrentTime() + " " + type.toUpperCase() + ": " + message);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean log(String message) {
        return info(message);
    }

    public static void shutdown() {
        isRunning = false;
        if (logThread == null) return;
        try {
            if (!logQueue.isEmpty() || logThread.isAlive()) logThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(LOG_FILE_PATH), StandardOpenOption.APPEND)) {
            bw.write(Kitimer.getCurrentTime() + " INFO: 日志线程已关闭");
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}