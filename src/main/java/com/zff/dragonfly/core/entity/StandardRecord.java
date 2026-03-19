package com.zff.dragonfly.core.entity;

import lombok.Data;

import java.util.Map;

@Data
public class StandardRecord {
    private String sourceType;    // 数据来源类型（如：SINA_REPORT）
    private String sourceId;      // 来源标识
    private long timestamp;       // 采集时间
    private String data;
    private Map<String, String> metadata; // 元数据（如：原始协议、采集节点IP等）

    public static StandardRecord of(String sourceType, String sourceId, String jsonData) {
        StandardRecord record = new StandardRecord();
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setData(jsonData);
        return record;
    }
    // 链式添加元数据
    public StandardRecord addMeta(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }
}
