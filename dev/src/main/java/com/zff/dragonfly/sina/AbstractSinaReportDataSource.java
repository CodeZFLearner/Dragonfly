package com.zff.dragonfly.sina;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasource.AbstractDataSource;
import com.zff.dragonfly.core.entity.StandardRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;

/**
 * 新浪财经财报数据源通用抽象类
 * 统一处理 HTTP 请求、JSON 解析、数据清洗
 * 子类只需指定：报表类型 (用于获取 Mapping 和 URL 参数)
 */
@Slf4j
public abstract class AbstractSinaReportDataSource extends AbstractDataSource {

//    private static final String API_TEMPLATE = "https://quotes.sina.cn/cn/api/openapi.php/CompanyFinanceService.getFinanceReport2022?paperCode=${code}&source=${source}&type=0&page=1&num=${num}";

    SinaFinanceUrl sinaUrlBuild;

    public AbstractSinaReportDataSource(DataSink sink, SinaFinanceUrl sinaUrlBuild) {
        super(sink);
        this.sinaUrlBuild = sinaUrlBuild;
        // 从配置中心获取对应的映射表
    }

    @Override
    protected List<StandardRecord> doFetch() throws Exception {

        List<StandardRecord> allRecords = new ArrayList<>();

        String url = sinaUrlBuild.toUrl();
        String code = sinaUrlBuild.getPaperCode();

        try {
            log.info("请求 URL: {}", url);
            String jsonStr = HttpUtil.get(url, null, 5000);
            if (jsonStr != null) {
                List<StandardRecord> records = parseResponse(code, jsonStr);
                allRecords.addAll(records);
            }
        } catch (Exception e) {
            log.error("抓取 {} 失败: {}", code, e.getMessage());
        }

        log.info("共获取 {} 条记录。", allRecords.size());
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
                String publishDate = reportDetail.getStr("publish_date");
                JSONArray items = reportDetail.getJSONArray("data");

                if (items == null || items.isEmpty()) continue;


                // todo

                boolean hasData = false;
                HashMap<String, Object> tempData = new HashMap<>();

                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String title = item.getStr("item_title");
                    String valueStr = item.getStr("item_value");

                    Object cleanVal = cleanValue(valueStr);
                    tempData.put(title, cleanVal);
                    if (cleanVal != null) hasData = true;
                }

                if (hasData) {
                    StandardRecord record = new StandardRecord();
                    record.setSourceType(sinaUrlBuild.getSource());
                    record.setTimestamp(System.currentTimeMillis());
                    // todo 标准对象
                    Map<String, Serializable> sData = Map.of("code",code,"publish_date", publishDate, "data", tempData);
                    record.setData(JSONUtil.toJsonStr(sData));
                    records.add(record);
                }
            }
        } catch (Exception e) {
            log.warn("解析 {} 的 {} JSON 失败: {}", code, sinaUrlBuild.getSource(), e.getMessage());
        }

        return records;
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
