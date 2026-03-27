package com.zff.dismantle.api.dto;

import com.zff.dismantle.core.DisclosureLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Expand request for getting more details about a specific chunk.
 */
@Data
@Schema(description = "扩展片段详情请求 v2 - 获取特定片段的更详细信息")
public class ExpandRequestV2 {

    @NotBlank(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID", example = "sess_abc123")
    private String sessionId;

    @NotBlank(message = "片段 ID 不能为空")
    @Schema(description = "要扩展的片段 ID", example = "sec_001")
    private String chunkId;

    @Schema(description = "目标披露级别",
            example = "SUMMARY",
            defaultValue = "SUMMARY",
            allowableValues = {"SUMMARY", "EXPANDED", "FULL"})
    private DisclosureLevel targetLevel = DisclosureLevel.SUMMARY;

    @Schema(description = "是否递归扩展子片段",
            example = "false",
            defaultValue = "false")
    private boolean recursive = false;
}
