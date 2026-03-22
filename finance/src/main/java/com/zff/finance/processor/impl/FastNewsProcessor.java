package com.zff.finance.processor.impl;

import com.zff.core.datasink.impl.ReturnSink;
import com.zff.core.entity.StandardRecord;
import com.zff.core.processor.Processor;
import com.zff.core.processor.ProcessorRequest;
import com.zff.finance.east.FastNewsDataSource;
import com.zff.finance.processor.entity.FastNewsVo;
import com.zff.finance.processor.request.FastNewsRequest;

import java.util.List;

public class FastNewsProcessor implements Processor<FastNewsVo> {
    @Override
    public boolean support(ProcessorRequest processorRequest) {
        return (processorRequest instanceof FastNewsRequest) && ((FastNewsRequest) processorRequest).enableFastNews();
    }

    @Override
    public List<FastNewsVo> process(ProcessorRequest processorRequest) {
        FastNewsRequest fastNewsRequest = (FastNewsRequest) processorRequest;
        if (fastNewsRequest.enableFastNews()) {
            // 获取快讯数据
            // 封装数据
            List<StandardRecord> run = new FastNewsDataSource(new ReturnSink()).run();

            return FastNewsVo.fromStandardRecord(run);
        }
        return null;
    }
}
