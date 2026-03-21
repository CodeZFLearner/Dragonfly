package com.zff.dragonfly.core.datasink.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.entity.StandardRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JsonFileSink implements DataSink {

    private final String filePath;
    private final List<StandardRecord> cache;

    public JsonFileSink(String filePath) {
        this.filePath = filePath;
        this.cache = new ArrayList<>();
    }

    @Override
    public void init() {
        File file = new File(filePath);
        FileUtil.mkdir(file.getParent());
        // 初始化时清空旧文件
        if (file.exists()) {
            file.delete();
        }
        log.info("JsonFileSink initialized: {}", filePath);
    }

    @Override
    public List<StandardRecord> transform(List<StandardRecord> records) {
        // 1. 缓存数据
        if (records != null && !records.isEmpty()) {
            cache.addAll(records);
        }
        // 2. 返回原数据（或者返回处理后的数据），保持链路畅通
        return records;
    }

    @Override
    public void close() {
        log.info("JsonFileSink closing. Writing {} records to {}", cache.size(), filePath);
        try {
            // 将缓存的所有数据转换为 JSON 字符串
            String jsonContent = JSONUtil.toJsonStr(cache);
            // 写入文件
            FileUtil.writeString(jsonContent, filePath, StandardCharsets.UTF_8);
            log.info("JsonFileSink finished successfully.");
        } catch (Exception e) {
            log.error("Failed to write JSON file", e);
            throw new RuntimeException("Write JSON failed", e);
        } finally {
            cache.clear();
        }
    }
}
