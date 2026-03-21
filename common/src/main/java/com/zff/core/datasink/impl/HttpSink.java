package com.zff.core.datasink.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.zff.core.datasink.DataSink;
import com.zff.core.entity.StandardRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpSink implements DataSink {

    private final String url;
    private final Map<String, String> headers;
    private final List<StandardRecord> cache;
    // 控制是否在 transform 阶段就发送（流式），还是 close 时发送（批量）
    private final boolean flushOnTransform;

    /**
     * @param url 目标地址
     * @param headers 请求头
     * @param flushOnTransform true: 每批数据来了就发（低延迟，高网络开销）; false: 累积到 close 再发（高吞吐，占用内存）
     */
    public HttpSink(String url, Map<String, String> headers, boolean flushOnTransform) {
        this.url = url;
        this.headers = headers != null ? headers : new HashMap<>();
        this.cache = new ArrayList<>();
        this.flushOnTransform = flushOnTransform;
    }

    public HttpSink(String url) {
        this(url, null, false);
    }

    @Override
    public void init() {
        log.info("HttpSink initialized. Target URL: {}", url);
        // 可以在这里测试连通性
    }

    @Override
    public List<StandardRecord> transform(List<StandardRecord> records) {
        if (records == null || records.isEmpty()) {
            return records;
        }

        if (flushOnTransform) {
            // 流式模式：立即发送
            sendHttpRequest(records);
        } else {
            // 批量模式：缓存
            cache.addAll(records);
        }

        return records;
    }

    @Override
    public void close() {
        if (!flushOnTransform && !cache.isEmpty()) {
            log.info("HttpSink closing. Sending remaining {} records...", cache.size());
            sendHttpRequest(cache);
            cache.clear();
        } else if (flushOnTransform) {
            log.info("HttpSink closing. All data was flushed during transform.");
        } else {
            log.info("HttpSink closing. No data to send.");
        }
    }

    private void sendHttpRequest(List<StandardRecord> data) {
        try {
            // 将数据转换为 JSON 字符串作为 Body
            String jsonBody = JSONUtil.toJsonStr(data);

            log.debug("Sending HTTP POST to {}: {}", url, jsonBody.length() > 100 ? jsonBody.substring(0, 100) + "..." : jsonBody);

            // 使用 Hutool 原生 HttpUtil 发送，也可以调用之前封装的 HttpUtils.postJson
            HttpResponse response = HttpUtil.createPost(url)
                    .addHeaders(headers)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .execute();

            if (response.isOk()) {
                log.info("HTTP Send Success. Status: {}, Response: {}", response.getStatus(), response.body());
            } else {
                log.error("HTTP Send Failed. Status: {}, Response: {}", response.getStatus(), response.body());
                // 根据业务需求，失败是否抛异常？这里选择抛出以中断流程
                throw new RuntimeException("HTTP request failed with status: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Error sending data to HTTP sink", e);
            throw new RuntimeException("HTTP Sink error", e);
        }
    }
}