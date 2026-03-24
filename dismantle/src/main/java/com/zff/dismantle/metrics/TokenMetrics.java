package com.zff.dismantle.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 使用统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token 使用统计 - 显示节省效果")
public class TokenMetrics {

    @Schema(description = "原始文档的估算 Token 数", example = "2500")
    private int originalTokens;

    @Schema(description = "实际处理的 Token 数（选中内容 + 回答）", example = "1500")
    private int processedTokens;

    @Schema(description = "节省百分比", example = "40%")
    private String savings;

    @Schema(description = "选中的片段数量", example = "2")
    private int chunksSelected;

    @Schema(description = "总片段数量", example = "4")
    private int totalChunks;

    public static TokenMetrics of(int originalTokens, int processedTokens, int chunksSelected, int totalChunks) {
        double savingsPercent = originalTokens > 0
                ? ((double) (originalTokens - processedTokens) / originalTokens) * 100
                : 0;
        return TokenMetrics.builder()
                .originalTokens(originalTokens)
                .processedTokens(processedTokens)
                .savings(String.format("%.0f%%", Math.max(0, savingsPercent)))
                .chunksSelected(chunksSelected)
                .totalChunks(totalChunks)
                .build();
    }
}
