package com.zff.finance.east;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.core.datasink.DataSink;
import com.zff.core.datasource.AbstractDataSource;
import com.zff.core.entity.StandardRecord;
import com.zff.finance.processor.entity.FastNewsVo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 东方财富快讯数据源实现 7 * 24
 * 接口地址：https://np-seclist.eastmoney.com/sec/getFastNews
 * 请求方式：POST JSON
 */
public class FastNewsDataSource extends AbstractDataSource {

    private static final String FAST_NEWS_URL = "https://np-seclist.eastmoney.com/sec/getFastNews";

    // 快讯特有字段 (可根据实际需求补充，这里复用了部分通用字段并增加了快讯特有字段)
    private static final String[] FAST_NEWS_FIELDS = {
            "artTitle", "artCode", "summary", "mediaName", "showTime",
            "pinglunNum", "clickNum", "shareNum", "title", "content",
            "source", "publishTime", "id", "url"
            // 如果需要通用字段如 f12, f14 等，也可以加在这里
    };

    public FastNewsDataSource(DataSink sink) {
        super(sink);
    }

    @Override
    protected List<StandardRecord> doFetch() throws Exception {
        // 1. 构建请求参数 (对应 Go: param := map[string]interface{}{...})
        // 注意：Go 代码中 timestamp 来自 query，这里如果没有外部传入，通常使用当前时间戳

        // 默认 pageSize 为 200 (对应 Go 逻辑)
        int pageSize = 100;

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("biz", "sec_724");
        paramMap.put("client", "sec_android");
        paramMap.put("h24ColumnCode", "102");
        paramMap.put("order", 2);
        paramMap.put("pageSize", pageSize);
//        paramMap.put("timestamp", timestamp);
        paramMap.put("trace", "fd189d7e-02b7-456e-ac47-6ac93ee1484b"); // 固定 trace 或生成随机 UUID

        // 2. 发送 POST 请求 (Body 为 JSON)
        // Hutool 自动将 Map 序列化为 JSON 字符串，并设置 Content-Type: application/json
        String responseBody = HttpRequest.post(FAST_NEWS_URL)
                .header("Content-Type", "application/json")
                .header("User-Agent", "sec_android") // 模拟客户端
                .body(JSONUtil.toJsonStr(paramMap))  // 关键：将 Map 转为 JSON 字符串作为 Body
                .timeout(5000)
                .execute()
                .body();

        // 3. 解析响应
        return parseResponseData(responseBody);
    }

    /**
     * 解析快讯响应数据
     * 逻辑与 EventStrategy 类似，但路径可能不同，需根据实际返回调整
     */
    private List<StandardRecord> parseResponseData(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return new ArrayList<>();
        }

        JSONObject root = JSONUtil.parseObj(jsonStr);
        JSONArray dataList = null;

        Object byPath = root.getByPath("data.items");

        List<StandardRecord> resultList = new ArrayList<>();

        if (byPath instanceof JSONArray) {
            dataList = (JSONArray) byPath;
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject jsonObject = dataList.getJSONObject(i);

                resultList.add(FastNewsVo.toStandardRecord(jsonObject));

            }
        }


        return resultList;
    }

    public static void main(String[] args) {
        new FastNewsDataSource(null).run();
    }
}
