package com.zff.dragonfly.core.processor;

import lombok.RequiredArgsConstructor;

import java.util.List;

public class ProcessorEngine<T extends  ProcessorResponse> {
    private final T response;
    private final List<Processor<? extends ProcessorResult>> processors;

    public ProcessorEngine(T response, List<Processor<? extends ProcessorResult>> processors) {
        this.response = response;
        this.processors = processors;
    }

    public T process(ProcessorRequest processorRequest) {
        for (Processor<? extends ProcessorResult> processor : processors) {
            if (processor.support(processorRequest)) {
                ProcessorResult processorResult = processor.process(processorRequest);
                response.setResult(processorResult);
            }
        }
        return response;
    }
}
