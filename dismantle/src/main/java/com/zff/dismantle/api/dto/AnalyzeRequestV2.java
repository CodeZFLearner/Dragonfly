package com.zff.dismantle.api.dto;

import com.zff.dismantle.core.DisclosureLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Stage A analyze request for v2 API with progressive disclosure support.
 */
@Data
@Schema(description = "文本分析请求 v2 - 支持渐进式披露")
public class AnalyzeRequestV2 {

    @NotBlank(message = "文本内容不能为空")
    @Schema(description = "需要分析的长文本内容",
            example = "这是一篇很长的文章，包含了多个段落和章节...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "分块策略：semantic(按语义分割), outline(仅提取结构), fixed(按固定长度)",
            example = "semantic",
            defaultValue = "semantic")
    private String chunkStrategy = "semantic";

    @Schema(description = "披露级别：OUTLINE(仅标题), SUMMARY(标题 + 摘要), EXPANDED(扩展), FULL(完整内容)",
            example = "OUTLINE",
            defaultValue = "OUTLINE")
    private DisclosureLevel disclosureLevel = DisclosureLevel.OUTLINE;

    @Schema(description = "文档标题 (可选，用于元数据)",
            example = "项目需求文档")
    private String documentTitle;

    @Schema(description = "固定长度策略下的块大小（字符数），仅当 chunkStrategy=fixed 时使用",
            example = "1000",
            defaultValue = "1000")
    private Integer chunkSize;

    @Schema(description = "是否启用摘要生成 (需要额外的处理时间)",
            example = "true",
            defaultValue = "false")
    private boolean enableSummary = false;
}
