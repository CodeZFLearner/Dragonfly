package com.zff.finance.mvc;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;



@Data
@Schema(description = "财经报表查询请求参数")
public class SinaFinanceQuery {

    @Schema(description = "股票代码", example = "600519", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "股票代码不能为空")
    private String code;

    @Schema(description = "数据来源标识:gjzb-关键指标,fzb-资产负债表,lrb-利润表,llb-现金流量表", example = "gjzb", defaultValue = "gjzb")
    private String source;

    @Schema(description = "报表类型：0-全部, 1-一季报, 2-中报, 3-三季报, 4-年报", example = "0", defaultValue = "0")
    @Min(value = 0, message = "类型最小为0")
    @Max(value = 4, message = "类型最大为4")
    private Integer type;

    @Schema(description = "页码", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    @Min(value = 1, message = "条数最小为1")
    @Max(value = 100, message = "条数最大为100")
    private Integer num = 4;
}