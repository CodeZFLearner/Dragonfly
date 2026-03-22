package com.zff.finance.processor.entity;

import com.zff.core.processor.ProcessorResult;
import com.zff.finance.east.EastMoneyContentService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "东方财富新闻数据")
public class EastNewsVo implements ProcessorResult {
    private String code;
    private String title;
    private String showTime;
    private String mediaName;

    public static List<EastNewsVo> fromNewsResponse(EastMoneyContentService.NewsResponse newsResponse) {
        return newsResponse.getData().getNews().stream().map(item -> {
            EastNewsVo vo = new EastNewsVo();
            vo.setCode(item.getCode());
            vo.setTitle(item.getTitle());
            vo.setShowTime(item.getShowTime());
            vo.setMediaName(item.getMediaName());
            return vo;
        }).toList();
    }
}
