package com.zff.finance.mvc;

import cn.hutool.json.JSONUtil;
import com.zff.core.datasink.impl.ReturnSink;
import com.zff.core.entity.StandardRecord;
import com.zff.core.processor.Processor;
import com.zff.core.processor.ProcessorEngine;
import com.zff.finance.processor.impl.EastNewsProcessor;
import com.zff.finance.processor.impl.FastNewsProcessor;
import com.zff.finance.processor.impl.FutureEventProcessor;
import com.zff.finance.processor.impl.QAProcessor;
import com.zff.finance.sina.FinanceFieldConfig;
import com.zff.finance.sina.KeyReportDataSource;
import com.zff.finance.sina.SinaFinanceUrl;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestMapping("/finance")
@RestController
public class FinanceController {

    @Operation(summary = "财务数据综合查询接口", description = "支持快讯、未来事件、财报等多维度数据的综合查询")
    @PostMapping("/composite/query")
    public FinanceCompositeVo financeComposite(@RequestBody FinanceCompositeQuery query) {
        FinanceCompositeVo financeCompositeVo = new FinanceCompositeVo();
        List<Processor<?>> processors = List.of(new FastNewsProcessor(), new FutureEventProcessor(), new QAProcessor(), new EastNewsProcessor());
        ProcessorEngine<FinanceCompositeVo> engine = new ProcessorEngine<>(financeCompositeVo,processors);
        engine.process(query);
        if(query.getSinaFinanceQuery() != null){
            SinaFinanceQuery sinaFinanceQuery = query.getSinaFinanceQuery();
            FinanceFieldConfig.ReportType bySourceCode = FinanceFieldConfig.ReportType.getBySourceCode(sinaFinanceQuery.getSource());
            SinaFinanceUrl url = SinaFinanceUrl.builder()
                    .paperCode(sinaFinanceQuery.getCode())
                    .source(bySourceCode.getSourceCode())
                    .type(sinaFinanceQuery.getType())
                    .page(sinaFinanceQuery.getPage())
                    .num(sinaFinanceQuery.getNum())
                    .build();
            List<StandardRecord> run = new KeyReportDataSource(new ReturnSink(), url).run();
            List<Map<String , Object>> reports = new ArrayList<>();
            run.forEach(record -> {
                Map<String, Object> data = JSONUtil.toBean(record.getData(), Map.class);
                reports.add(data);
            });
            financeCompositeVo.setReports(reports);
        }
        return financeCompositeVo;
    }

}
