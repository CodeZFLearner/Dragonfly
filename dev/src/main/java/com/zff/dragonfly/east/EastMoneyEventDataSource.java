package com.zff.dragonfly.east;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasource.AbstractDataSource;
import com.zff.dragonfly.core.entity.StandardRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Hutool 简化的东方财富事件数据源实现
 * 代码量减少约 60%，去除了繁琐的 IO 流和原生 JSON 解析逻辑
 */
public class EastMoneyEventDataSource extends AbstractDataSource {

    private static final String EVENT_URL_TEMPLATE = "https://np-listapi.eastmoney.com/sec/economiccalendar/findEventByDateRange?startDate={}&eventType=001&pageNum=1&pageSize=15&req_trace=";

    // 需要提取的字段列表
    private static final String[] EAST_MONEY_FIELDS = {
            "sc", "f14", "f12", "f2", "f3", "f24",
            "CHANGE_DATE", "CHANGE_AMOUNT", "POSITION_NAME", "AVERAGE_PRICE",
            "artTitle", "artCode", "summary", "mediaName", "showTime",
            "pinglunNum", "clickNum", "shareNum", "name", "code", "title", "realSort"
    };

    public EastMoneyEventDataSource(DataSink sink) {
        super(sink);
    }

    @Override
    protected List<StandardRecord> doFetch() throws Exception {
        // 1. 构建 URL (Hutool StrUtil.format 支持 {} 占位符)
        String today = DateUtil.today(); // 直接获取 yyyy-MM-dd
        String requestUrl = StrUtil.format(EVENT_URL_TEMPLATE, today);

        System.out.println("Requesting: " + requestUrl);

        // 2. 发送请求并获取响应体字符串 (Hutool HttpRequest 链式调用)
        // 自动处理 IO 流关闭、编码转换等底层细节
        String responseBody = HttpRequest.get(requestUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(5000) // 设置超时 5 秒
                .execute()
                .body();

        // 3. 解析 JSON 并转换为标准记录列表
        return parseResponseData(responseBody);
    }

    /**
     * 使用 Hutool JSONUtil 简化解析逻辑
     */
    private List<StandardRecord> parseResponseData(String jsonStr) {
        // 检查空响应
        if (StrUtil.isBlank(jsonStr)) {
            return new ArrayList<>();
        }

        JSONObject root = JSONUtil.parseObj(jsonStr);
        JSONArray dataList = null;

        Object byPath = root.getByPath("data.importantEventList");
        if (byPath instanceof JSONArray) {
            dataList = (JSONArray) byPath;
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject jsonObject = dataList.getJSONObject(i);
                String name = jsonObject.getStr("name");
                String id = jsonObject.getStr("id");
                String showTime = jsonObject.getStr("showTime");
                System.out.println("Event " + i + ": name=" + name + ", id=" + id);
            }
        }


        if (dataList == null || dataList.isEmpty()) {
            System.out.println("[EastMoney]: 未找到有效数据列表");
            return new ArrayList<>();
        }

        List<StandardRecord> resultList = new ArrayList<>(dataList.size());


        return resultList;
    }

    public static void main(String[] args) {
        new EastMoneyEventDataSource(null).run();
    }
}
