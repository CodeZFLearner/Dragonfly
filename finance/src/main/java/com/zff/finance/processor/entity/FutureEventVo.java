package com.zff.finance.processor.entity;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.core.entity.StandardRecord;
import com.zff.core.processor.ProcessorResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "未来将要发生事件")
public class FutureEventVo implements ProcessorResult{
    @Schema(description = "事件名称")
    private String title;
    @Schema(description = "事件发生时间")
    private String showTime;

    public static StandardRecord toStandardRecord(JSONObject jsonObject) {
        String name = jsonObject.getStr("name");
        String id = jsonObject.getStr("id");
        String showTime = jsonObject.getStr("showTime");

        FutureEventVo futureEventVo = new FutureEventVo();
        futureEventVo.setTitle(name);
        futureEventVo.setShowTime(showTime);

        StandardRecord standardRecord = new StandardRecord();
        standardRecord.setData(JSONUtil.toJsonStr(futureEventVo));
        return standardRecord;
    }
    public static FutureEventVo fromStandardRecord(StandardRecord standardRecord) {
        return JSONUtil.toBean(standardRecord.getData(),FutureEventVo.class);
    }
     public static List<FutureEventVo> fromStandardRecord(List<StandardRecord> standardRecord) {
        return standardRecord.stream().map(FutureEventVo::fromStandardRecord).toList();
    }
}
