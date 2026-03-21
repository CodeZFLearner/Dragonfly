package com.zff.dragonfly.sina;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeUtils {

    /**
     * 将输入的股票代码转换为新浪财经接口标准格式 (小写前缀 + 6位数字)
     * 支持格式：
     * 1. "600519" (纯数字) -> 自动识别为 "sh600519"
     * 2. "sh600519", "SH600519" (带前缀) -> 标准化为 "sh600519"
     * 3. "600519.SH" (后缀格式) -> 标准化为 "sh600519"
     *
     * @param code 原始代码
     * @return 标准化后的代码 (如 "sh600519")，如果无法识别或为空则返回 null
     */
    public static String toSinaPaperCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        // 1. 预处理：去空格，转大写以便统一判断后缀
        String cleanCode = code.trim().toUpperCase();
        String numericPart;
        String exchangePrefix = null;

        // 2. 解析后缀格式 (如 "600519.SH", "000001.SZ")
        if (cleanCode.endsWith(".SH")) {
            numericPart = cleanCode.substring(0, cleanCode.length() - 3);
            exchangePrefix = "sh";
        } else if (cleanCode.endsWith(".SZ")) {
            numericPart = cleanCode.substring(0, cleanCode.length() - 3);
            exchangePrefix = "sz";
        }
        // 3. 解析前缀格式 (如 "SH600519", "sz000001")
        else if (cleanCode.startsWith("SH")) {
            numericPart = cleanCode.substring(2);
            exchangePrefix = "sh";
        } else if (cleanCode.startsWith("SZ")) {
            numericPart = cleanCode.substring(2);
            exchangePrefix = "sz";
        }
        // 4. 纯数字格式 (如 "600519") -> 需要自动推断交易所
        else if (cleanCode.matches("\\d+")) {
            numericPart = cleanCode;
            // 确保是6位，如果不是6位尝试补零或报错（视业务需求而定，这里假设输入已是6位或接近）
            if (numericPart.length() < 6) {
                // 可选：左侧补零到6位，例如 "1" -> "000001"
                numericPart = String.format("%06d", Long.parseLong(numericPart));
            }
            exchangePrefix = inferExchange(numericPart);
        }
        // 其他未知格式
        else {
            log.warn("无法识别的股票代码格式: {}", code);
            return null;
        }

        // 校验数字部分是否合法（必须全是数字）
        if (!numericPart.matches("\\d+")) {
            log.warn("代码包含非数字字符: {}", numericPart);
            return null;
        }

        // 如果推断失败（例如数字不在已知范围内）
        if (exchangePrefix == null) {
            log.warn("无法根据代码 {} 推断交易所，请输入带后缀的代码", code);
            return null;
        }

        // 5. 返回标准格式：小写前缀 + 6位数字
        return exchangePrefix + numericPart;
    }

    /**
     * 根据代码前缀推断交易所
     * @param code 6位数字代码
     * @return "sh", "sz" 或 null (无法识别)
     */
    private static String inferExchange(String code) {
        if (code.length() < 2) return null;

        String prefix2 = code.substring(0, 2);
        String prefix1 = code.substring(0, 1);

        // 上海规则: 60, 68 (主板/科创), 5 (部分基金/债券)
        if ("60".equals(prefix2) || "68".equals(prefix2) || "5".equals(prefix1)) {
            return "sh";
        }

        // 深圳规则: 00, 30 (主板/创业), 1 (部分基金/债券), 2 (旧B股)
        if ("00".equals(prefix2) || "30".equals(prefix2) || "1".equals(prefix1) || "2".equals(prefix1)) {
            return "sz";
        }

        // 北交所 (北京证券交易所): 8 开头 (如 83, 87, 43 等，新浪有时用 bj 或归入 sz/sh，需确认具体接口)
        // 注：新浪接口对北交所支持可能是 "bj" 前缀，或者仍归类在特定逻辑下。
        // 如果接口不支持北交所，这里返回 null；如果支持，可添加:
        if ("8".equals(prefix1) || "4".equals(prefix1) || "9".equals(prefix1)) {
            // 根据实际情况，新浪有时用 'bj'，有时北交所数据在特殊板块。
            // 保守起见，如果不确定接口是否支持 'bj'，可以返回 null 或根据文档调整。
            // 假设当前接口主要关注沪深，若需支持北交所请取消下面注释：
            // return "bj";
            log.debug("检测到疑似北交所代码 {}, 默认返回 null (需确认接口支持)", code);
            return null;
        }

        return null;
    }

    // --- 测试用例 ---
    public static void main(String[] args) {
        String[] tests = {
                "600519",       // 纯数字 -> sh600519
                "000001",       // 纯数字 -> sz000001
                "300750",       // 纯数字 -> sz300750
                "688001",       // 纯数字 -> sh688001
                "sh600519",     // 前缀 -> sh600519
                "SZ000001",     // 前缀大写 -> sz000001
                "600519.SH",    // 后缀 -> sh600519
                "000001.sz",    // 后缀小写 -> sz000001
                "  600519  ",   // 带空格 -> sh600519
                "123456",       // 未知规则 -> null
                null            // 空值 -> null
        };

        for (String test : tests) {
            System.out.printf("输入: %-12s -> 输出: %s%n",
                    "\"" + test + "\"",
                    toSinaPaperCode(test)
            );
        }
    }
}
