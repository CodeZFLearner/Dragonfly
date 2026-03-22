package com.zff.dragonfly.controller;


import com.zff.dragonfly.core.processor.Processor;
import com.zff.dragonfly.core.processor.ProcessorEngine;
import com.zff.dragonfly.core.processor.ProcessorResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/test")
@RestController
public class TestController {
    @RequestMapping("/hello")
    public CompositeResponse hello(CompositeRequest compositeRequest) {
        List<Processor<? extends ProcessorResult>> processors = List.of(new P1(), new P2());
        ProcessorEngine<CompositeResponse> compositeResponseProcessorEngine = new ProcessorEngine<>(new CompositeResponse(), processors);
        return compositeResponseProcessorEngine.process(compositeRequest);
    }
}
