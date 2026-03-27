package com.zff.dismantle.api.dto;

import com.zff.dismantle.core.ChunkLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chunk view for v2 API - supports hierarchical and progressive disclosure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "片段信息视图 v2 - 支持分层和渐进式披露")
public class ChunkViewV2 {

    @Schema(description = "片段 ID", example = "sec_001")
    private String id;

    @Schema(description = "片段标题", example = "项目背景与目标")
    private String title;

    @Schema(description = "片段级别", example = "SECTION",
            allowableValues = {"DOCUMENT", "SECTION", "SUBSECTION", "PARAGRAPH"})
    private ChunkLevel level;

    @Schema(description = "父片段 ID (如果有层级关系)", example = "sec_001")
    private String parentId;

    @Schema(description = "子片段 ID 列表", example = "[\"sub_001\", \"sub_002\"]")
    private java.util.List<String> childIds;

    @Schema(description = "摘要内容 (仅当披露级别 >= SUMMARY 时返回)",
            example = "本项目旨在开发一个智能数据分析系统...")
    private String summary;

    @Schema(description = "关键词列表 (如果有)",
            example = "[\"数据分析\", \"机器学习\", \"API\"]")
    private java.util.List<String> keywords;

    @Schema(description = "完整内容 (仅当披露级别 = FULL 时返回)",
            example = "完整的片段内容文本...")
    private String content;

    @Schema(description = "字符数", example = "1500")
    private int charCount;

    @Schema(description = "估算 Token 数", example = "375")
    private int estimatedTokens;

    /**
     * Creates a view at OUTLINE level (minimal data).
     */
    public static ChunkViewV2 outline(String id, String title, ChunkLevel level) {
        return ChunkViewV2.builder()
                .id(id)
                .title(title)
                .level(level)
                .build();
    }

    /**
     * Creates a view at OUTLINE level from existing view.
     */
    public static ChunkViewV2 toOutlineView(ChunkViewV2 full) {
        return ChunkViewV2.builder()
                .id(full.id)
                .title(full.title)
                .level(full.level)
                .parentId(full.parentId)
                .childIds(full.childIds)
                .charCount(full.charCount)
                .build();
    }

    /**
     * Creates a view at SUMMARY level.
     */
    public static ChunkViewV2 toSummaryView(ChunkViewV2 full) {
        return ChunkViewV2.builder()
                .id(full.id)
                .title(full.title)
                .level(full.level)
                .parentId(full.parentId)
                .childIds(full.childIds)
                .summary(full.summary)
                .charCount(full.charCount)
                .build();
    }

    /**
     * Checks if this view has content (FULL level).
     */
    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    /**
     * Checks if this view has summary (SUMMARY level or higher).
     */
    public boolean hasSummary() {
        return summary != null && !summary.isBlank();
    }
}
