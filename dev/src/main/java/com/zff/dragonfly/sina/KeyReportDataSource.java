package com.zff.dragonfly.sina;

import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasink.impl.PrintSink;

import java.util.List;

public class KeyReportDataSource extends AbstractSinaReportDataSource {

    public KeyReportDataSource(DataSink sink, SinaFinanceUrl sinaUrlBuild) {
        super(sink, sinaUrlBuild);
    }

    public static void main(String[] args) {
        try {
            SinaFinanceUrl url = SinaFinanceUrl.builder()
                    .paperCode("sh600519")
                    .source(FinanceFieldConfig.ReportType.CASH_FLOW.getSourceCode())
                    .type(1) // 0 全部
                    .page(1)
                    .num(10)
                    .build();
            new KeyReportDataSource(new PrintSink(),url).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
