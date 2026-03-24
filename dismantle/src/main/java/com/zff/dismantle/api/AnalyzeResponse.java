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
 * Stage A 分析响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文本分析响应 - 返回会话 ID 和片段列表")
public class AnalyzeResponse {

    @Schema(description = "会话 ID，用于后续生成请求", example = "sess_abc123")
    private String sessionId;

    @Schema(description = "片段列表（仅 ID + 标题，不包含全文内容）")
    private List<ChunkView> chunks;

    @Schema(description = "Token 使用统计")
    private TokenMetrics metrics;
}
