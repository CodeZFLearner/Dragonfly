package com.zff.dismantle.chunk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 片段视图 - 仅包含 ID 和标题（用于 API 响应，最小化 Token 暴露）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "片段信息视图 - 仅包含 ID 和标题，不包含全文内容")
public class ChunkView {

    @Schema(description = "片段 ID", example = "chunk_001")
    private String id;

    @Schema(description = "片段标题（概括性描述）", example = "项目背景与目标")
    private String title;
}
