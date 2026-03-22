package com.zff.dragonfly.east;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zff.dragonfly.core.datasink.DataSink;
import com.zff.dragonfly.core.datasource.AbstractDataSource;
import com.zff.dragonfly.core.entity.StandardRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 东方财富高管增减持数据源 (ShareStrategy)
 * 对应 Go: ShareStrategy
 * 接口: https://datacenter.eastmoney.com/securities/api/data/get
 * 逻辑: 获取原始数据 -> 按股票代码分组 -> 计算加权均价和总变动量 -> 排序取前10 -> 输出增持/减持榜单
 */
public class EastMoneyShareChangeDataSource extends AbstractDataSource {

    private static final Logger logger = LoggerFactory.getLogger(EastMoneyShareChangeDataSource.class);

    // 硬编码的 URL (对应 Go 常量 LastestChangeShares)
    // 注意：URL 中包含 type, sty, st, sr, p, ps 等参数
    private static final String SHARE_CHANGE_URL = "https://datacenter.eastmoney.com/securities/api/data/get" +
            "?type=RPT_EXECUTIVE_HOLD_DETAILS" +
            "&sty=CHANGE_DATE,SECURITY_CODE,PERSON_NAME,CHANGE_SHARES,AVERAGE_PRICE,CHANGE_AMOUNT,CHANGE_RATIO,CHANGE_AFTER_HOLDNUM,HOLD_TYPE,DSE_PERSON_NAME,POSITION_NAME,PERSON_DSE_RELATION" +
            "&sr=-1,-1" +
            "&st=CHANGE_DATE,CHANGE_AMOUNT" +
            "&extraCols=f2,f12,f14" +
            "&p=1" +
            "&ps=1000";

    /**
     * 构造函数
     */
    public EastMoneyShareChangeDataSource(DataSink sink) {
        super(sink);
    }

    @Override
    protected List<StandardRecord> doFetch() throws Exception {
        logger.debug("Requesting Share Change data from: {}", SHARE_CHANGE_URL);

        // 1. 发送 GET 请求
        String responseBody = HttpRequest.get(SHARE_CHANGE_URL)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", "https://data.eastmoney.com/")
                .timeout(15000) // 数据量可能较大，设置稍长超时
                .execute()
                .body();

        // 2. 解析并聚合数据
        return parseAndAggregateData(responseBody);
    }

    /**
     * 解析响应，执行分组、加权计算、排序和截断逻辑
     * 对应 Go: ParseResponse + merageShares
     */
    private List<StandardRecord> parseAndAggregateData(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            logger.warn("[EastMoney ShareChange] 响应体为空");
            return new ArrayList<>();
        }

        JSONObject root = JSONUtil.parseObj(jsonStr);

        JSONArray rawDataList = (JSONArray)root.getByPath("result.data");

        if (rawDataList == null || rawDataList.isEmpty()) {
            logger.info("[EastMoney ShareChange] 未获取到原始增减持记录");
            return new ArrayList<>();
        }

        // 分组容器
        Map<String, List<JSONObject>> increGroup = new HashMap<>();
        Map<String, List<JSONObject>> decreGroup = new HashMap<>();

        // 1. 遍历原始数据并分组
        for (int i = 0; i < rawDataList.size(); i++) {
            JSONObject item = rawDataList.getJSONObject(i);
            if (item == null) continue;

            // 获取变动金额 CHANGE_AMOUNT
            Double changeAmountNum = item.getDouble("CHANGE_AMOUNT");
            if (changeAmountNum == null) continue;

            double changeAmount = changeAmountNum;
            String f12 = item.getStr("f12"); // 股票代码

            if (StrUtil.isBlank(f12)) continue;

            if (changeAmount > 0) {
                increGroup.computeIfAbsent(f12, k -> new ArrayList<>()).add(item);
            } else {
                decreGroup.computeIfAbsent(f12, k -> new ArrayList<>()).add(item);
            }
        }

        // 2. 合并计算 (加权平均价 & 总金额)
        List<Map<String, Object>> increList = mergeShares(increGroup);
        List<Map<String, Object>> decreList = mergeShares(decreGroup);

        // 3. 排序 (按变动金额绝对值降序)
        Comparator<Map<String, Object>> amountComparator = (a, b) -> {
            double valA = Math.abs(((Number) a.get("CHANGE_AMOUNT")).doubleValue());
            double valB = Math.abs(((Number) b.get("CHANGE_AMOUNT")).doubleValue());
            return Double.compare(valB, valA); // 降序
        };

        increList.sort(amountComparator);
        decreList.sort(amountComparator);

        logger.info("[EastMoney ShareChange] 处理完成: 增持标的 {} 个, 减持标的 {} 个", increList.size(), decreList.size());

        // 5. 封装结果
        // 策略：生成两条 StandardRecord
        // Record 1: 类型=INCREASE, 数据=increList
        // Record 2: 类型=DECREASE, 数据=decreList
        List<StandardRecord> result = new ArrayList<>();

        if (!increList.isEmpty()) {
            StandardRecord increRecord = new StandardRecord();

        }

        if (!decreList.isEmpty()) {
            StandardRecord decreRecord = new StandardRecord();

        }

        return result;
    }

    /**
     * 合并同一标的的多条记录
     * 逻辑：累加 CHANGE_AMOUNT, 计算加权平均价 (SUM(amount*price)/SUM(amount))
     * 对应 Go: merageShares
     */
    private List<Map<String, Object>> mergeShares(Map<String, List<JSONObject>> group) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (List<JSONObject> shares : group.values()) {
            if (shares.isEmpty()) continue;

            double totalAmount = 0.0;
            double weightedSum = 0.0;

            // 获取基础信息 (从第一条取即可，同一代码名称不变)
            JSONObject firstItem = shares.get(0);
            String f12 = firstItem.getStr("f12");
            String f14 = firstItem.getStr("f14");
            String f2 = firstItem.getStr("f2"); // 最新价?

            for (JSONObject share : shares) {
                double changeAmount = share.getDouble("CHANGE_AMOUNT", 0.0);
                double averagePrice = share.getDouble("AVERAGE_PRICE", 0.0);

                totalAmount += changeAmount;
                weightedSum += changeAmount * averagePrice;
            }

            // 避免除以零
            double finalAvgPrice = (totalAmount != 0) ? (weightedSum / totalAmount) : 0.0;

            // 构建结果对象 (保留两位小数)
            Map<String, Object> obj = new HashMap<>();
            obj.put("CHANGE_AMOUNT", Math.round(totalAmount * 100.0) / 100.0);
            obj.put("AVERAGE_PRICE", Math.round(finalAvgPrice * 100.0) / 100.0);
            obj.put("f12", f12);
            obj.put("f14", f14);
            obj.put("f2", f2);

            result.add(obj);
        }

        return result;
    }

    public static void main(String[] args) {
        new EastMoneyShareChangeDataSource(null).run();
    }
}
