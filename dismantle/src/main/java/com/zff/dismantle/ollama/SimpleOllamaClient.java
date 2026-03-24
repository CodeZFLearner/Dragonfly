package com.zff.dismantle.ollama;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper; // 需要引入 jackson-databind 用于 JSON 解析

public class SimpleOllamaClient {

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public SimpleOllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.client = HttpClient.newHttpClient();
    }

    public String generate(String model, String prompt) throws Exception {
        // 构建请求体
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "temperature", 0.2,
                "stream", false // 关闭流式，直接返回完整结果
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API error: " + response.body());
        }

        // 解析响应 (简化版，实际生产环境建议定义 DTO 类)
        Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
        return (String) result.get("response");
    }

    public String nameTitle(String prompt){
        try {
            String basePrompt = "一个合格的标题应满足以下标准：\n" +
                    "1.必须真实反映核心内容，不夸大、不误导。\n" +
                    "2.去除冗余词汇（如“关于...的报告”、“一种...的方法”），直击要害\n" +
                    "3.包含利益点、冲突点或新奇点，激发阅读欲望\n" +
                    "4.包含用户可能搜索的关键词，便于归档和查找。\n" +
                    "这是一段截取的段落:\n" +
                    "<content>%s</content> 为他命名一个标题, just output a title ,no others";
//            System.out.println(prompt);
            return generate("gemma2:2b", String.format(basePrompt, prompt));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        SimpleOllamaClient ollamaClient = new SimpleOllamaClient("http://localhost:11434");

        try {
            String text = ollamaClient.generate("gemma2:2b", "Hello, who are you?");
            System.out.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
