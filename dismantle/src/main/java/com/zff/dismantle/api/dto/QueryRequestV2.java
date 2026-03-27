package com.zff.dismantle.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Query request for searching within a document.
 */
@Data
@Schema(description = "文档内查询请求 v2 - 支持关键词搜索")
public class QueryRequestV2 {

    @NotBlank(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID", example = "sess_abc123")
    private String sessionId;

    @NotBlank(message = "查询关键词不能为空")
    @Schema(description = "查询关键词或短语", example = "技术方案")
    private String query;

    @Schema(description = "最大返回结果数量",
            example = "10",
            defaultValue = "10")
    @Size(max = 50, message = "最大返回结果数不能超过 50")
    private Integer maxResults = 10;

    @Schema(description = "最小相关分数阈值",
            example = "0.3",
            defaultValue = "0.3")
    private Double minScore = 0.3;

    @Schema(description = "是否高亮匹配内容",
            example = "true",
            defaultValue = "false")
    private boolean highlight = false;

    @Schema(description = "高亮标签前缀",
            example = "<mark>",
            defaultValue = "<mark>")
    private String highlightPrefix = "<mark>";

    @Schema(description = "高亮标签后缀",
            example = "</mark>",
            defaultValue = "</mark>")
    private String highlightSuffix = "</mark>";
}
