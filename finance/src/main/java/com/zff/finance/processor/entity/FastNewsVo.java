package com.zff.finance.processor.entity;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.core.entity.StandardRecord;
import com.zff.core.processor.ProcessorResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "快讯数据")
@Data
public class FastNewsVo implements ProcessorResult {
    @Schema(description = "快讯ID")
    private String code;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "摘要")
    private String digest;
    @Schema(description = "更新时间")
    private String time;
    @Schema(description = "快讯来源")
    private String source;

    // 其他

    public static StandardRecord toStandardRecord(JSONObject jsonObject) {
        String title = jsonObject.getStr("title");
        // digest
        String digest = jsonObject.getStr("digest");
        String code = jsonObject.getStr("code");
        // source
        String source = jsonObject.getStr("source");
        String timestamp = jsonObject.getStr("realSort");
        //readCount
        Integer readCount = jsonObject.getInt("readCount");
        //commentCount
        Integer commentCount = jsonObject.getInt("commentCount");
        // shareCount
        Integer shareCount = jsonObject.getInt("shareCount");


        FastNewsVo fastNewsVo = new FastNewsVo();
        fastNewsVo.setTitle(title);
        fastNewsVo.setDigest(digest);
        fastNewsVo.setCode(code);
        fastNewsVo.setSource(source);
        fastNewsVo.setTime(timestamp);

        StandardRecord standardRecord = new StandardRecord();
        standardRecord.setData(JSONUtil.toJsonStr(fastNewsVo));
        return standardRecord;
    }

    public static FastNewsVo fromStandardRecord(StandardRecord standardRecord) {
        return JSONUtil.toBean(standardRecord.getData(),FastNewsVo.class);
    }
    public static List<FastNewsVo> fromStandardRecord(List<StandardRecord> standardRecord) {
        return standardRecord.stream().map(FastNewsVo::fromStandardRecord).toList();
    }
}
