package com.zff.finance.mvc;

import com.zff.finance.processor.request.EastNewsRequest;
import com.zff.finance.processor.request.FastNewsRequest;
import com.zff.finance.processor.request.FutureEventRequest;
import com.zff.finance.processor.request.QARequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Schema(description = "财经综合查询请求参数")
@Data
public class FinanceCompositeQuery implements FastNewsRequest, FutureEventRequest, QARequest, EastNewsRequest {

    @Schema(description = "新浪财经查询参数")
    private SinaFinanceQuery sinaFinanceQuery;
    @Schema(description = "是否启用快讯查询")
    private Boolean enableFastNews;
    @Schema(description = "是否获取董秘QA数据")
    private Boolean enableQA;
    @Schema(description = "是否获取东方财富快讯数据")
    private Boolean enableEastNews;
    @Schema(description = "股票代码", example = "600519", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    @Min(value = 1, message = "条数最小为1")
    @Max(value = 100, message = "条数最大为100")
    private Integer pageSize;

    @Schema(description = "是否启用未来事件查询")
    private Boolean enableFutureEvent;
    @Override
    public boolean enableFastNews() {
        return enableFastNews != null && enableFastNews;
    }

    @Override
    public boolean enableFutureEvent() {
        return enableFutureEvent != null && enableFutureEvent;
    }

    @Override
    public boolean enableQA() {
        return enableQA != null && enableQA;
    }

    @Override
    public boolean enableEastNews() {
        return enableEastNews != null && enableEastNews;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Integer getPageSize() {
        return pageSize;
    }
}
