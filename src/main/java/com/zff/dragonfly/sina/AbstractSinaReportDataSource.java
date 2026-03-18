package com.zff.dragonfly.sina;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasource.AbstractDataSource;
import com.zff.dragonfly.core.entity.StandardRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 新浪财经财报数据源通用抽象类
 * 统一处理 HTTP 请求、JSON 解析、数据清洗
 * 子类只需指定：报表类型 (用于获取 Mapping 和 URL 参数)
 */
@Slf4j
public abstract class AbstractSinaReportDataSource extends AbstractDataSource {

    private static final String API_TEMPLATE = "https://quotes.sina.cn/cn/api/openapi.php/CompanyFinanceService.getFinanceReport2022?paperCode=${code}&source=${source}&type=0&page=1&num=${num}";

    List<String> stockPool;
    protected final FinanceFieldConfig.ReportType reportType;
    protected final int historyNum;
    protected final Map<String, String> currentMapping;

    public AbstractSinaReportDataSource(DataSink sink, List<String> stockPool, int historyNum, FinanceFieldConfig.ReportType type) {
        super(sink);
        this.stockPool = stockPool;
        this.historyNum = historyNum;
        this.reportType = type;
        // 从配置中心获取对应的映射表
        this.currentMapping = FinanceFieldConfig.getMapping(type);

        if (this.currentMapping.isEmpty()) {
            log.warn("报表类型 {} 未配置任何字段映射，可能导致抓取的数据为空。", type);
        }
    }

    @Override
    protected List<StandardRecord> doFetch() throws Exception {
        String typeName = reportType.name();
        log.info("[{}] 开始抓取，股票数: {}, 历史期数: {}", typeName, stockPool.size(), historyNum);

        List<StandardRecord> allRecords = new ArrayList<>();
        String sourceParam = FinanceFieldConfig.getSourceParam(reportType);
        String urlTemplate = API_TEMPLATE.replace("${source}", sourceParam);

        for (String code : stockPool) {
            String sinaCode = toSinaPaperCode(code);
            if (sinaCode == null) continue;

            String url = urlTemplate.replace("${code}", sinaCode).replace("${num}", String.valueOf(historyNum));

            try {
                log.info("[{}] 请求 URL: {}", typeName, url);
                String jsonStr = HttpUtil.get(url, null, 5000);
                if (jsonStr != null) {
                    List<StandardRecord> records = parseResponse(code, jsonStr);
                    allRecords.addAll(records);
                }
            } catch (Exception e) {
                log.error("[{}] 抓取 {} 失败: {}", typeName, code, e.getMessage());
            }

            // 简单限流
            if (stockPool.size() > 5) {
                Thread.sleep(80);
            }
        }

        log.info("[{}] 完成，共获取 {} 条记录。", typeName, allRecords.size());
        return allRecords;
    }

    /**
     * 通用解析逻辑
     */
    private List<StandardRecord> parseResponse(String code, String jsonStr) {
        List<StandardRecord> records = new ArrayList<>();

        try {
            JSONObject root = JSONUtil.parseObj(jsonStr);
            JSONObject result = root.getJSONObject("result");
            if (result == null) return records;

            JSONObject data = result.getJSONObject("data");
            if (data == null) return records;

            JSONObject reportList = data.getJSONObject("report_list");
            if (reportList == null) return records;

            for (String reportDate : reportList.keySet()) {
                JSONObject reportDetail = reportList.getJSONObject(reportDate);
                JSONArray items = reportDetail.getJSONArray("data");

                if (items == null || items.isEmpty()) continue;


                // todo

                boolean hasData = false;
                HashMap<String, Object> tempData = new HashMap<>();

                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String title = item.getStr("item_title");
                    String valueStr = item.getStr("item_value");

                    // 使用注入的 currentMapping 进行匹配
                    if (currentMapping.containsKey(title)) {
                        String standardKey = currentMapping.get(title);
                        Object cleanVal = cleanValue(valueStr);
                        tempData.put(standardKey, cleanVal);
                        if (cleanVal != null) hasData = true;
                    }
                }

                if (hasData) {
                    StandardRecord record = new StandardRecord();
                    record.setSourceType(reportType.name());
                    record.setTimestamp(System.currentTimeMillis());
                    record.setData(JSONUtil.toJsonStr(tempData));
                    records.add(record);
                }
            }
        } catch (Exception e) {
            log.warn("解析 {} 的 {} JSON 失败: {}", code, reportType.name(), e.getMessage());
        }

        return records;
    }

    private String toSinaPaperCode(String code) {
        if (code == null) return null;
        code = code.toUpperCase().trim();
        if (code.endsWith(".SH") || code.endsWith(".SZ")) {
            return code.substring(0, code.length() - 3);
        }
        return code;
    }

    private Object cleanValue(String val) {
        if (val == null || val.trim().isEmpty() || "null".equals(val) || "--".equals(val)) {
            return null;
        }
        try {
            String numStr = val.replace(",", "");
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return val;
        }
    }
}
