package com.zff.dismantle.api.dto;

import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.metrics.TokenMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Retrieve response for v2 API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "片段检索/合并响应 v2")
public class RetrieveResponseV2 {

    @Schema(description = "合并后的完整文本",
            example = "[项目背景与目标]\n本项目旨在开发一个智能数据分析系统...\n\n[技术方案设计]\n采用微服务架构...")
    private String mergedText;

    @Schema(description = "选中的片段列表")
    private List<ChunkViewV2> selectedChunks;

    @Schema(description = "实际返回的披露级别",
            example = "FULL")
    private DisclosureLevel disclosureLevel;

    @Schema(description = "Token 使用统计")
    private TokenMetrics metrics;

    @Schema(description = "处理信息 (如包含了多少个子片段)",
            example = "Expanded 2 sections with 5 child paragraphs")
    private String processingInfo;
}
