package com.zff.dragonfly.core.processor;

public interface Processor<T extends ProcessorResult> {
    boolean support(ProcessorRequest processorRequest);
    T process(ProcessorRequest processorRequest);
}
