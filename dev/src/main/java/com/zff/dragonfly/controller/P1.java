package com.zff.dragonfly.controller;

import com.zff.dragonfly.core.processor.Processor;
import com.zff.dragonfly.core.processor.ProcessorRequest;
import com.zff.dragonfly.core.processor.ProcessorResult;

public class P1 implements Processor<ResultA> {
    @Override
    public boolean support(ProcessorRequest processorRequest) {
        return true;
    }

    @Override
    public ResultA process(ProcessorRequest processorRequest) {
        return new ResultA();
    }
}
