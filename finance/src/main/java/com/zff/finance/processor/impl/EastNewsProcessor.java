package com.zff.finance.processor.impl;

import com.zff.core.processor.Processor;
import com.zff.core.processor.ProcessorRequest;
import com.zff.finance.east.EastMoneyContentService;
import com.zff.finance.processor.entity.EastNewsVo;
import com.zff.finance.processor.request.EastNewsRequest;

import java.util.List;

public class EastNewsProcessor implements Processor<EastNewsVo> {
    @Override
    public boolean support(ProcessorRequest processorRequest) {
        return (processorRequest instanceof EastNewsRequest) && ((EastNewsRequest) processorRequest).enableEastNews();
    }

    @Override
    public List<EastNewsVo> process(ProcessorRequest processorRequest) {

        EastNewsRequest newsRequest = (EastNewsRequest) processorRequest;
        EastMoneyContentService eastMoneyContentService = new EastMoneyContentService();
        EastMoneyContentService.NewsResponse newsResponse = eastMoneyContentService.handleNews(newsRequest.getCode(), newsRequest.getPageSize());
        return EastNewsVo.fromNewsResponse(newsResponse);
    }
}
