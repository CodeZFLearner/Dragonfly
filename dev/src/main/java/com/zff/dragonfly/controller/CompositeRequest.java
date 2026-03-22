package com.zff.dragonfly.controller;

import com.zff.dragonfly.core.processor.ProcessorRequest;
import com.zff.dragonfly.core.processor.ProcessorType;

import java.util.List;

public class CompositeRequest implements ProcessorRequest {
    @Override
    public List<ProcessorType> getProcessorTypes() {
        return List.of();
    }
}
