package com.zff.core.datasink.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zff.core.datasink.DataSink;
import com.zff.core.entity.StandardRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TxtFileSink implements DataSink {

    private final String filePath;
    private final List<String> lineCache;

    public TxtFileSink(String filePath) {
        this.filePath = filePath;
        this.lineCache = new ArrayList<>();
    }

    @Override
    public void init() {
        File file = new File(filePath);
        FileUtil.mkdir(file.getParent());
        if (file.exists()) {
            file.delete();
        }
        log.info("TxtFileSink initialized: {}", filePath);
    }

    @Override
    public List<StandardRecord> transform(List<StandardRecord> records) {
        if (records == null || records.isEmpty()) {
            return records;
        }

        // 将 Record 转换为字符串行并缓存
        // 假设 StandardRecord 有 toString() 或者有 getId() 等方法，这里演示用 toString
        // 实际使用中建议显式提取字段：record.getId() + "," + record.getName()
        List<String> lines = records.stream()
                .map(JSONUtil::toJsonStr)
                .toList();

        lineCache.addAll(lines);
        return records;
    }

    @Override
    public void close() {
        log.info("TxtFileSink closing. Writing {} lines to {}", lineCache.size(), filePath);
        try {
            // 使用换行符连接所有行
            String content = StrUtil.join(System.lineSeparator(), lineCache);
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
            log.info("TxtFileSink finished successfully.");
        } catch (Exception e) {
            log.error("Failed to write TXT file", e);
            throw new RuntimeException("Write TXT failed", e);
        } finally {
            lineCache.clear();
        }
    }
}
