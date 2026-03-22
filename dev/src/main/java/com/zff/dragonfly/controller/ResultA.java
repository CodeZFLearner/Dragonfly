package com.zff.dragonfly.controller;

import com.zff.dragonfly.core.processor.ProcessorResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "ResultA")
public class ResultA implements ProcessorResult {
    @Schema(description = "id")
    private String id;
    @Schema(description = "排序")
    private int order;
    @Schema(description = "项目")
    private String project;
    @Schema(description = "分数")
    private double score;
}
