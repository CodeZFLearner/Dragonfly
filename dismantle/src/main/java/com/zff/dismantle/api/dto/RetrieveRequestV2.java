package com.zff.dismantle.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Retrieve request for v2 API with query support.
 */
@Data
@Schema(description = "片段检索请求 v2 - 支持查询和过滤")
public class RetrieveRequestV2 {

    @NotNull(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID，由 /analyze 接口返回", example = "sess_abc123")
    private String sessionId;

    @NotEmpty(message = "至少选择一个片段")
    @Schema(description = "选中的片段 ID 列表",
            example = "[\"sec_001\", \"sec_003\"]")
    private List<String> chunkIds;

    @Schema(description = "是否包含子片段 (如果选中片段有 children)",
            example = "true",
            defaultValue = "true")
    private boolean includeChildren = true;

    @Schema(description = "是否添加格式标记 (如 [Title] 前缀)",
            example = "true",
            defaultValue = "true")
    private boolean addFormatMarkers = true;
}
