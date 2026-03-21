package com.zff.sina.core;


import com.zff.core.datasink.DataSink;
import com.zff.core.datasink.impl.PrintSink;

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
