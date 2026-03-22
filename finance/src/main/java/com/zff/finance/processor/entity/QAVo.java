package com.zff.finance.processor.entity;

import com.zff.core.processor.ProcessorResult;
import com.zff.finance.east.EastMoneyContentService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "获取董秘问答 (QA)")
public class QAVo implements ProcessorResult {

    @Schema(description = "问题")
    private String question;
    @Schema(description = "答案")
    private String answer;


    public static List<QAVo> fromQAResponse(EastMoneyContentService.QAResponse qa) {
        return qa.getRe().stream().map(item -> {
            QAVo vo = new QAVo();
            vo.setQuestion(item.getAskQuestion());
            vo.setAnswer(item.getAskAnswer());
            return vo;
        }).toList();
    }
}
