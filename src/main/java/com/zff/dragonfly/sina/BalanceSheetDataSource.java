package com.zff.dragonfly.sina;

import com.zff.dragonfly.core.datasink.DataSink;

import java.util.List;

public class BalanceSheetDataSource extends AbstractSinaReportDataSource {

    public BalanceSheetDataSource(DataSink sink, List<String> stockPool, int historyNum) {
        super(sink, stockPool, historyNum, FinanceFieldConfig.ReportType.BALANCE_SHEET);
    }
}
