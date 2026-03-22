package com.zff.dragonfly.controller;

import com.zff.dragonfly.core.processor.ProcessorResult;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ResultB")
@Data
public class ResultB implements ProcessorResult {
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "年龄")
    private int age;
    @Schema(description = "性别")
    private String sex;
}
