package com.zff.finance.processor.impl;

import com.zff.core.datasink.impl.ReturnSink;
import com.zff.core.processor.Processor;
import com.zff.core.processor.ProcessorRequest;
import com.zff.finance.east.FutureEventDataSource;
import com.zff.finance.processor.entity.FutureEventVo;
import com.zff.finance.processor.request.FutureEventRequest;

import java.util.List;

public class FutureEventProcessor implements Processor<FutureEventVo> {
    @Override
    public boolean support(ProcessorRequest processorRequest) {
        return (processorRequest instanceof FutureEventRequest) && ((FutureEventRequest) processorRequest).enableFutureEvent();
    }

    @Override
    public List<FutureEventVo> process(ProcessorRequest processorRequest) {
        FutureEventRequest futureEventRequest = (FutureEventRequest) processorRequest;
        if (futureEventRequest.enableFutureEvent()) {
            // 获取未来事件数据
            // 封装数据
            return FutureEventVo.fromStandardRecord(new FutureEventDataSource(new ReturnSink()).run());
        }
        return null;
    }
}
