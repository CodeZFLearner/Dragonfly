package com.zff.finance.processor.impl;

import com.zff.core.processor.Processor;
import com.zff.core.processor.ProcessorRequest;
import com.zff.finance.east.EastMoneyContentService;
import com.zff.finance.processor.entity.QAVo;
import com.zff.finance.processor.request.QARequest;

import java.util.List;

public class QAProcessor implements Processor<QAVo> {
    @Override
    public boolean support(ProcessorRequest processorRequest) {
        return (processorRequest instanceof QARequest) && ((QARequest) processorRequest).enableQA();
    }

    @Override
    public List<QAVo> process(ProcessorRequest processorRequest) {

        QARequest qaRequest = (QARequest) processorRequest;
        if (qaRequest.enableQA()) {
            // 获取问答数据
            // 封装数据
            EastMoneyContentService eastMoneyContentService = new EastMoneyContentService();
            EastMoneyContentService.QAResponse qa = eastMoneyContentService.getQA(qaRequest.getCode(), qaRequest.getPageSize());
            return QAVo.fromQAResponse(qa);
        }
        return null;
    }
}
