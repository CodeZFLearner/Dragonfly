package com.zff.dragonfly.core.entity;

import lombok.Data;

import java.util.Map;

@Data
public class StandardRecord {
    private String sourceId;      // 来源标识
    private long timestamp;       // 采集时间
    private String data;
    private Map<String, String> metadata; // 元数据（如：原始协议、采集节点IP等）
}
