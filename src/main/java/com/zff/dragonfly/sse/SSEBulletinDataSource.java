package com.zff.dragonfly.sse;

import cn.hutool.core.date.DateException;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasink.impl.PrintSink;
import com.zff.dragonfly.core.datasource.AbstractDataSource;
import com.zff.dragonfly.core.entity.StandardRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上交所公告数据源 (基于 queryCompanyBulletinNew.do 接口)
 * 支持构建者模式配置参数，自动处理 JSONP 解析
 */
@Slf4j
public class SSEBulletinDataSource extends AbstractDataSource {

    private static final String SOURCE_TYPE = "SSE_BULLETIN";
    private static final String API_URL = "https://query.sse.com.cn/security/stock/queryCompanyBulletinNew.do";

    // 固定的 JSONP 回调函数名，方便解析
    private static final String JSONP_CALLBACK_NAME = "fetchCallback";

    private final QueryParams queryParams;

    /**
     * 构造函数
     * @param sink 数据接收器
     * @param queryParams 查询参数配置
     */
    public SSEBulletinDataSource(DataSink sink, QueryParams queryParams) {
        super(sink);
        this.queryParams = queryParams;
    }

    public static void main(String[] args) {
        // 示例：抓取贵州茅台 2023 年的公告
        QueryParams params = QueryParams.builder()
                .securityCode("600009")
//                .startDate("2026-01-01")
//                .endDate("2026-03-19")
                .pageSize(25) // 每页数量
                .pageNo(1) // 页码
                .build();

        SSEBulletinDataSource dataSource = new SSEBulletinDataSource(new PrintSink(), params);
        try {
            dataSource.run();
        } catch (Exception e) {
            log.error("抓取失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected List<StandardRecord> doFetch() throws Exception {
        log.info("[{}] 开始抓取，股票: {}, 日期范围: {} ~ {}",
                SOURCE_TYPE,
                queryParams.getSecurityCode(),
                queryParams.getStartDate(),
                queryParams.getEndDate());

        List<StandardRecord> allRecords = new ArrayList<>();

        try {
            // 1. 构建请求参数
            Map<String, Object> params = buildRequestParamMap();

            // 2. 发送 GET 请求
            String responseBody;
            try (HttpResponse response = HttpRequest.get(API_URL)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://www.sse.com.cn/disclosure/listedinfo/announcement/")
                    .form(params) // Hutool 自动将 Map 转为 URL 查询参数
                    .timeout(5000)
                    .execute()) {

                if (!response.isOk()) {
                    throw new RuntimeException("HTTP 请求失败: " + response.getStatus());
                }
                responseBody = response.body();
            }

            // 3. 清洗 JSONP 并解析
            String jsonString = cleanJsonp(responseBody, JSONP_CALLBACK_NAME);
            if (jsonString == null || jsonString.trim().isEmpty()) {
                log.warn("[{}] 响应内容为空或格式错误", SOURCE_TYPE);
                return allRecords;
            }

            JSONObject root = JSONUtil.parseObj(jsonString);

            // 4. 提取数据列表
            // 注意：根据实际抓包，数据通常在 'result' 数组中
            JSONArray resultList = root.getJSONArray("result");
            if (resultList == null || resultList.isEmpty()) {
                log.info("[{}] 未获取到数据", SOURCE_TYPE);
                return allRecords;
            }

            log.debug("[{}] 当前页获取到 {} 条记录", SOURCE_TYPE, resultList.size());

            // 5. 转换为 StandardRecord
            for (int i = 0; i < resultList.size(); i++) {
                JSONArray item = resultList.getJSONArray(i);
                if (item == null || item.isEmpty()) {
                    continue;
                }
                for (int j = 0; j < item.size(); j++){
                    JSONObject obj = item.getJSONObject(j);
                    if (obj == null || obj.isEmpty()) {
                        continue;
                    }
                    StandardRecord record = convertToRecord(obj);
                    if (record != null) {
                        allRecords.add(record);
                    }
                }
            }

        } catch (Exception e) {
            log.error("[{}] 抓取异常: {}", SOURCE_TYPE, e.getMessage(), e);
            throw e; // 抛出异常让上层处理重试或停止
        }

        log.info("[{}] 完成，共获取 {} 条记录。", SOURCE_TYPE, allRecords.size());
        return allRecords;
    }

    /**
     * 构建请求参数 Map
     */
    private Map<String, Object> buildRequestParamMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("jsonCallBack", JSONP_CALLBACK_NAME);
        params.put("isPagination", "true");
        params.put("pageHelp.pageSize", String.valueOf(queryParams.getPageSize()));
        params.put("pageHelp.pageNo", String.valueOf(queryParams.getPageNo()));
        params.put("pageHelp.cacheSize", "1");
        params.put("pageHelp.beginPage", "1");
        params.put("pageHelp.endPage", "1");

        // 业务过滤参数
        if(queryParams.getSecurityCode() != null && !queryParams.getSecurityCode().isEmpty()){
            params.put("SECURITY_CODE", queryParams.getSecurityCode());
        }
        if(queryParams.getStockType() != null && !queryParams.getStockType().isEmpty()){
            params.put("START_DATE", queryParams.getStockType());
        }
        params.put("START_DATE", queryParams.getStartDate());
        params.put("END_DATE", queryParams.getEndDate());

        params.put("_", System.currentTimeMillis()); // 防缓存时间戳

        return params;
    }

    /**
     * 转换单条数据为标准记录
     */
    private StandardRecord convertToRecord(JSONObject item) {
        // 唯一ID：通常使用公告ID或 代码+日期+标题 的组合
        // 上交所返回中可能有 'SSE_CODE' 或 'ANNOUNCEMENT_ID'，这里假设用 TITLE + DATE 做简易去重，实际请根据字段调整
        String id = item.getStr("SSE_CODE");
        if (id == null || id.isEmpty()) {
            // 如果没有唯一ID，尝试组合
            id = item.getStr("SECURITY_CODE") + "_" + item.getStr("SSE_DATE") + "_" + item.getStr("TITLE");
        }

        if (id == null) return null;

        Map<String,String> data = new HashMap<>();

        data.put("SSEDATE", item.getStr("SSEDATE"));
        data.put("SECURITY_CODE", item.getStr("SECURITY_CODE"));
        data.put("SECURITY_NAME", item.getStr("SECURITY_NAME"));
        data.put("TITLE", item.getStr("TITLE"));
        data.put("URL", item.getStr("URL"));

        // 原始数据序列化
        String jsonData = JSONUtil.toJsonStr(data);

        StandardRecord record = StandardRecord.of(SOURCE_TYPE, id, jsonData);
        return record;
    }

    /**
     * 构建详情页 URL (根据实际规则拼接，此处为示例)
     * 通常需要结合 SECURITY_CODE 和 文件路径，具体需参考前端 JS 逻辑
     */
    private String buildDetailUrl(JSONObject item) {
        String code = item.getStr("SECURITY_CODE");
        String date = item.getStr("SSE_DATE");
        // 注意：真实的 URL 构建逻辑可能比较复杂，需要查看页面 JS 如何生成链接
        // 这里提供一个通用的占位符，实际使用时请替换为正确的拼接逻辑
        return String.format("https://www.sse.com.cn/disclosure/listedinfo/announcement/c/new/%s/%s.shtml",
                date != null ? date.replace("-", "") : "latest", code);
    }

    /**
     * 清洗 JSONP 包装
     */
    private String cleanJsonp(String response, String callbackName) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        String prefix = callbackName + "(";
        String suffix = ")";

        String trimmed = response.trim();
        if (trimmed.startsWith(prefix) && trimmed.endsWith(suffix)) {
            return trimmed.substring(prefix.length(), trimmed.length() - suffix.length());
        }
        // 如果不是 JSONP，直接返回（可能是纯 JSON 或错误 HTML）
        return trimmed;
    }

    // ==========================================================
    // 内部静态类：查询参数配置 (使用 Lombok Builder)
    // ==========================================================

    @Data
    @Builder
    public static class QueryParams {
        // 字段定义
        private String securityCode;
        private String startDate;
        private String endDate;
        private String bulletinTypes = "00,0101,0102,0104,0103";
        private String stockType = "";
        private int pageNo = 1;
        private int pageSize = 25;
        private int maxPages = 1;
    }
}
