package com.zff.core.processor;

import java.util.List;
public class ProcessorEngine<T extends  ProcessorResponse> {
    private final T response;
    private final List<Processor<? extends ProcessorResult>> processors;

    public ProcessorEngine(T response, List<Processor<? extends ProcessorResult>> processors) {
        this.response = response;
        this.processors = processors;
    }

    public T process(ProcessorRequest processorRequest) {
        // todo 并行，流式而非list
        for (Processor<? extends ProcessorResult> processor : processors) {
            if (processor.support(processorRequest)) {
                List<? extends ProcessorResult> processorResult = processor.process(processorRequest);

                for (ProcessorResult result : processorResult) {
                    response.setResult(result);
                }
            }
        }
        return response;
    }
}
