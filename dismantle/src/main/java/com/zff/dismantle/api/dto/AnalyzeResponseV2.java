package com.zff.dismantle.api.dto;

import com.zff.dismantle.core.ChunkLevel;
import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.metrics.TokenMetrics;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Stage A analyze response for v2 API with progressive disclosure support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文本分析响应 v2 - 返回分层片段信息")
public class AnalyzeResponseV2 {

    @Schema(description = "会话 ID，用于后续请求", example = "sess_abc123")
    private String sessionId;

    @Schema(description = "文档 ID", example = "doc_xyz789")
    private String documentId;

    @Schema(description = "文档标题 (如果提供或自动提取)", example = "项目需求文档")
    private String documentTitle;

    @Schema(description = "当前披露级别", example = "OUTLINE")
    private DisclosureLevel disclosureLevel;

    @Schema(description = "根级别片段列表 (根据披露级别返回不同 detail)",
            example = """
                    [
                      {"id": "sec_001", "title": "项目背景", "level": "SECTION"},
                      {"id": "sec_002", "title": "技术方案", "level": "SECTION"}
                    ]
                    """)
    private List<ChunkViewV2> chunks;

    @Schema(description = "总片段数量 (所有级别)", example = "15")
    private int totalChunkCount;

    @Schema(description = "Token 使用统计")
    private TokenMetrics metrics;

    @Schema(description = "可用操作提示",
            example = "Use GET /api/dismantle/v2/session/{id}/expand/{chunkId} to get more details")
    private String expandHint;
}
