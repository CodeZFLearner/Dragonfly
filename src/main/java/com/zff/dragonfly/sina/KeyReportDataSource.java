package com.zff.dragonfly.sina;

import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasink.impl.PrintSink;

import java.util.List;

public class KeyReportDataSource extends AbstractSinaReportDataSource {

    public KeyReportDataSource(DataSink sink, List<String> stockPool, int historyNum) {
        super(sink, stockPool, historyNum, FinanceFieldConfig.ReportType.KEY_INDICATORS);
    }

    public static void main(String[] args) {
        try {
            new KeyReportDataSource(new PrintSink(),
                    List.of("sh600000", "sz000001"), 10).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
