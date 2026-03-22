package com.zff.finance.mvc;

import com.zff.core.processor.ProcessorResponse;
import com.zff.core.processor.ProcessorResult;
import com.zff.finance.processor.entity.FastNewsVo;
import com.zff.finance.processor.entity.FutureEventVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "财务数据综合响应对象，包含多个维度的财务信息")
public class FinanceCompositeVo implements ProcessorResponse {
    @Schema(description = "财务数据列表")
    List<Map<String,Object>> reports;

    @Schema(description = "快讯数据")
    private List<FastNewsVo> fastNewsData = new ArrayList<>();
    @Schema(description = "未来将要发生事件")
    private List<FutureEventVo> futureEventData = new ArrayList<>();

    @Override
    public void setResult(ProcessorResult processorResult) {
        if (processorResult instanceof FastNewsVo r1) {
            this.fastNewsData.add(r1);
        } else if (processorResult instanceof FutureEventVo r2) {
            this.futureEventData.add(r2);
        }
    }
}
