package com.zff.dismantle.api.dto;

import com.zff.dismantle.metrics.TokenMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Query response for document search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档内查询响应 v2")
public class QueryResponseV2 {

    @Schema(description = "查询关键词", example = "技术方案")
    private String query;

    @Schema(description = "匹配的片段列表 (按相关性排序)",
            example = """
                    [
                      {
                        "chunkId": "sec_002",
                        "title": "技术方案设计",
                        "score": 0.95,
                        "snippet": "采用微服务架构...",
                        "highlightedSnippet": "采用<mark>微服务</mark>架构..."
                      }
                    ]
                    """)
    private List<QueryResultView> results;

    @Schema(description = "匹配结果数量", example = "3")
    private int resultCount;

    @Schema(description = "Token 使用统计")
    private TokenMetrics metrics;

    /**
     * View for a single query result.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询结果视图")
    public static class QueryResultView {

        @Schema(description = "片段 ID", example = "sec_002")
        private String chunkId;

        @Schema(description = "片段标题", example = "技术方案设计")
        private String title;

        @Schema(description = "片段级别", example = "SECTION")
        private String level;

        @Schema(description = "相关分数 (0-1)", example = "0.95")
        private Double score;

        @Schema(description = "匹配片段预览",
                example = "采用微服务架构，使用 Spring Boot 作为基础框架...")
        private String snippet;

        @Schema(description = "高亮后的匹配片段",
                example = "采用<mark>微服务</mark>架构，使用<mark>Spring Boot</mark>作为基础框架...")
        private String highlightedSnippet;

        @Schema(description = "片段字符数", example = "1500")
        private int charCount;
    }
}
