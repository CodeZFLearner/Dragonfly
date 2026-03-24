package com.zff.dismantle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Stage A 分析请求
 */
@Data
@Schema(description = "文本分析请求 - 用于分割长文本为片段")
public class AnalyzeRequest {

    @NotBlank(message = "文本内容不能为空")
    @Schema(description = "需要分析的长文本内容",
            example = "这是一篇很长的文章，包含了多个段落和章节...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "分块策略：semantic(按语义分割) 或 fixed(按固定长度)",
            example = "semantic",
            defaultValue = "semantic")
    private String chunkStrategy = "semantic";

    @Schema(description = "固定长度策略下的块大小（字符数），仅当 chunkStrategy=fixed 时使用",
            example = "1000",
            defaultValue = "1000")
    private Integer chunkSize;
}
