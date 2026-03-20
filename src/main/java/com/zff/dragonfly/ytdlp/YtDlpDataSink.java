package com.zff.dragonfly.ytdlp;

import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.entity.StandardRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class YtDlpDataSink implements DataSink {
    private static final Logger logger = Logger.getLogger(YtDlpDataSink.class.getName());

    // 注入的配置请求对象 (不可变)
    private final YtDlpCommand baseRequest;

    // 线程池，用于并发下载多个视频
    private ExecutorService executor;

    // 是否启用并发处理
    private final boolean parallelProcessing;
    private final int threadPoolSize;

    /**
     * 构造器注入 YtDlpRequest
     * @param baseRequest 基础配置模板 (URL 将被每条记录覆盖)
     * @param parallelProcessing 是否并行处理列表中的多个视频
     * @param threadPoolSize 并行线程数
     */
    public YtDlpDataSink(YtDlpCommand baseRequest, boolean parallelProcessing, int threadPoolSize) {
        this.baseRequest = baseRequest;
        this.parallelProcessing = parallelProcessing;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * 简化构造器：默认串行执行
     */
    public YtDlpDataSink(YtDlpCommand baseRequest) {
        this(baseRequest, false, 1);
    }

    @Override
    public void init() {
        logger.info("Initializing YtDlpDataSink...");

        // 1. 检查 yt-dlp 是否可用
        try {
            ProcessBuilder pb = new ProcessBuilder(baseRequest.getYtPath(), "--version");
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("yt-dlp not found or not executable. Exit code: " + exitCode);
            }
            logger.info("yt-dlp check passed.");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to initialize YtDlpDataSink: " + e.getMessage(), e);
        }

        // 2. 初始化线程池 (如果需要并行)
        if (parallelProcessing) {
            this.executor = Executors.newFixedThreadPool(threadPoolSize);
            logger.info("Executor initialized with " + threadPoolSize + " threads.");
        }
    }

    @Override
    public List<StandardRecord> transform(List<StandardRecord> records) {
        List<String> command = baseRequest.toCommandArgs();
        logger.info("Executing: " + String.join(" ", command));
        int i = executeCommand(command);
        return records;
    }

    // todo 提取工具类
    private int executeCommand(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = null;
        try {
            process = processBuilder.start();
            StringBuilder outputLog = new StringBuilder();

            // 读取输出流
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLog.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Success: " + baseRequest.getUrl());
            } else {
                logger.warning("Failed: " + baseRequest.getUrl() + " (Exit: " + exitCode + ")" + outputLog);
            }
            return exitCode;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void close() {
        logger.info("Shutting down YtDlpDataSink...");
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        YtDlpCommand request = YtDlpCommand.builder()
                .url("https://www.bilibili.com/video/BV1BrcozyEuR/?spm_id_from=333.1007.tianma.9-3-33.click&vd_source=d509cad482899382178948591df18b4d")
                .ytPath("D:\\work\\file\\yt-dlp.exe")
                .extractAudio( true)
                .build();
        YtDlpDataSink sink = new YtDlpDataSink(request);
        sink.transform(null);

    }
}
