package com.zff.dragonfly.controller;

import com.zff.dragonfly.core.processor.ProcessorResponse;
import com.zff.dragonfly.core.processor.ProcessorResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

@Schema(description = "CompositeResponse")
@Getter
public class CompositeResponse implements ProcessorResponse {
    @Schema(description = "ResultA")
    private ResultA result;
    @Schema(description = "ResultB")
    private ResultB resultB;
    @Override
    public void setResult(ProcessorResult processorResult) {
        if (processorResult instanceof ResultA) {
            this.result = (ResultA) processorResult;
        } else if (processorResult instanceof ResultB) {
            this.resultB = (ResultB) processorResult;
        }

    }
}
