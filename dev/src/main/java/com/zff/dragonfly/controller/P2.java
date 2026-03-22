package com.zff.dragonfly.controller;

import com.zff.dragonfly.core.processor.Processor;
import com.zff.dragonfly.core.processor.ProcessorRequest;

public class P2 implements Processor<ResultB> {

    @Override
    public boolean support(ProcessorRequest processorRequest) {
        return true;
    }

    @Override
    public ResultB process(ProcessorRequest processorRequest) {
        return new ResultB();
    }
}
