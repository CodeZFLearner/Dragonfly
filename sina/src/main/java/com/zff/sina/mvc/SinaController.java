package com.zff.sina.mvc;

import com.zff.core.datasink.impl.PrintSink;
import com.zff.core.datasink.impl.ReturnSink;
import com.zff.core.entity.StandardRecord;
import com.zff.sina.core.FinanceFieldConfig;
import com.zff.sina.core.KeyReportDataSource;
import com.zff.sina.core.SinaFinanceUrl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequestMapping("/sina")
@RestController
public class SinaController {
    @Operation(
            summary = "查询公司财务报表",
            description = "根据股票代码查询指定类型的财务报表数据。"
    )
    @GetMapping("/finance/data")
    public Object financeData( @Validated
                                   @Parameter(description = "查询参数对象", required = true)
                                   SinaFinanceQueryDTO dto) {
        FinanceFieldConfig.ReportType bySourceCode = FinanceFieldConfig.ReportType.getBySourceCode(dto.getSource());
        SinaFinanceUrl url = SinaFinanceUrl.builder()
                .paperCode(dto.getCode())
                .source(bySourceCode.getSourceCode())
                .type(dto.getType())
                .page(dto.getPage())
                .num(dto.getNum())
                .build();
        List<StandardRecord> run = new KeyReportDataSource(new ReturnSink(), url).run();

        return Map.of("data",run);
    }

}
