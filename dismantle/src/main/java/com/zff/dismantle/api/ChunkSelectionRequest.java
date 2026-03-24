package com.zff.dismantle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Stage B 检索请求 - 选择要合并的片段
 */
@Data
@Schema(description = "片段检索请求 - 指定要合并的片段 ID 列表")
public class ChunkSelectionRequest {

    @NotNull(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID，由 /analyze 接口返回", example = "sess_abc123")
    private String sessionId;

    @NotEmpty(message = "至少选择一个片段")
    @Schema(description = "选中的片段 ID 列表", example = "[\"chunk_001\", \"chunk_003\"]")
    private List<String> chunkIds;
}
