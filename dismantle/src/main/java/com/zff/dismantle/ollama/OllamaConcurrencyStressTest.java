package com.zff.dismantle.ollama;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OllamaConcurrencyStressTest {

    // ================= 配置区域 =================
    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL = "gemma2:2b";

    private static final int TOTAL_TASKS = 1;      // 总共发送多少个请求
    private static final int MAX_CONCURRENT = 4;    // 【核心】最大并发数 (根据显存调整: 4, 8, 16)
    // ===========================================

    public static void main(String[] args) {
        System.out.println("🚀 开始 DismantleAI 并发性能测试 (基于 SimpleOllamaClient)");
        System.out.println("📦 模型: " + MODEL);
        System.out.println("🔢 总任务数: " + TOTAL_TASKS);
        System.out.println("⚡ 最大并发数: " + MAX_CONCURRENT);
        System.out.println("--------------------------------------------------");

        SimpleOllamaClient client = new SimpleOllamaClient(OLLAMA_URL);

        // 信号量：控制同时进行的请求数量
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT);

        // 统计数据
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatencyMs = new AtomicLong(0);

        Instant globalStart = Instant.now();

        // 使用虚拟线程池 (Java 21+) 
        // 如果是 Java 11/17，请改为: Executors.newFixedThreadPool(MAX_CONCURRENT * 2)
        try  {
            ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT * 2);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < TOTAL_TASKS; i++) {
                final int taskId = i;
                final String prompt = "请简要总结这段话 (任务#" + taskId + "): " +
                        "本地大模型部署是降低 AI 成本的关键。通过智能拆分和路由，" +
                        "我们可以让小模型处理简单任务，大模型处理复杂推理。" +
                        "这是第 " + taskId + " 个测试片段，用于验证并发性能。";

                Future<?> future = executor.submit(() -> {
                    try {
                        semaphore.acquire(); // 获取锁，超过并发数会在此等待
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    long start = System.currentTimeMillis();
                    try {
                        client.generate(MODEL, prompt);
                        long end = System.currentTimeMillis();

                        successCount.incrementAndGet();
                        totalLatencyMs.addAndGet(end - start);
                        // System.out.println("✅ 任务 " + taskId + " 完成");
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.err.println("❌ 任务 " + taskId + " 失败: " + e.getMessage());
                    } finally {
                        semaphore.release(); // 释放锁
                    }
                });
                futures.add(future);
            }

            // 等待所有任务结束
            for (Future<?> f : futures) {
                try { f.get(); } catch (Exception e) { e.printStackTrace(); }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        Instant globalEnd = Instant.now();
        long totalDuration = Duration.between(globalStart, globalEnd).toMillis();

        // 输出报告
        System.out.println("--------------------------------------------------");
        System.out.println("🏁 测试完成");
        System.out.println("⏱️  总耗时: " + totalDuration + " ms (" + String.format("%.2f", totalDuration/1000.0) + " s)");
        System.out.println("✅ 成功: " + successCount.get());
        System.out.println("❌ 失败: " + failCount.get());

        if (successCount.get() > 0) {
            double avgLatency = (double) totalLatencyMs.get() / successCount.get();
            double rps = (double) successCount.get() / (totalDuration / 1000.0);

            System.out.println("📊 平均延迟: " + String.format("%.2f", avgLatency) + " ms");
            System.out.println("🚀 吞吐量: " + String.format("%.2f", rps) + " req/s");
        }
        System.out.println("--------------------------------------------------");

        // 简单分析
        if (totalDuration > TOTAL_TASKS * 1000) {
            System.out.println("⚠️  警告: 总耗时过长，可能是串行执行或显存不足导致频繁换页。");
            System.out.println("💡 建议: 尝试减小 MAX_CONCURRENT 到 4 或 2。");
        } else if (failCount.get() > 0) {
            System.out.println("⚠️  警告: 出现失败请求，可能是并发过高导致 Ollama 拒绝服务或超时。");
            System.out.println("💡 建议: 减小 MAX_CONCURRENT 或增加请求超时时间。");
        } else {
            System.out.println("✨ 表现良好！当前并发设置适合你的硬件。");
        }
    }
}
