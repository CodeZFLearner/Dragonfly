package com.zff.dismantle.api;

import com.zff.dismantle.chunk.ChunkView;
import com.zff.dismantle.metrics.TokenMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Stage B 响应 - 返回合并后的文本
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "片段合并响应 - 返回选中片段的完整合并文本")
public class MergedChunksResponse {

    @Schema(description = "合并后的完整文本（agent 可直接用于其 LLM 输入）",
            example = "[项目背景与目标]\n本项目旨在开发一个智能数据分析系统...\n\n[技术方案设计]\n采用微服务架构...")
    private String mergedText;

    @Schema(description = "选中的片段列表")
    private List<ChunkView> selectedChunks;

    @Schema(description = "Token 使用统计 - 显示节省效果")
    private TokenMetrics metrics;
}
