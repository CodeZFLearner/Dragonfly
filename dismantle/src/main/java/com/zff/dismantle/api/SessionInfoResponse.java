package com.zff.dismantle.api;

import com.zff.dismantle.chunk.ChunkView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 会话信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话信息响应 - 包含会话详情和片段列表")
public class SessionInfoResponse {

    @Schema(description = "会话 ID", example = "sess_abc123")
    private String sessionId;

    @Schema(description = "创建时间", example = "2026-03-24T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "过期时间", example = "2026-03-24T11:30:00Z")
    private Instant expiresAt;

    @Schema(description = "片段总数", example = "4")
    private int chunkCount;

    @Schema(description = "所有片段的 ID 和标题列表")
    private List<ChunkView> chunks;
}
