package com.zff.core.processor;


import java.util.List;

public interface Processor<T extends ProcessorResult> {
    boolean support(ProcessorRequest processorRequest);
    List<T> process(ProcessorRequest processorRequest);
}
