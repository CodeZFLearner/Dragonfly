package com.zff.dragonfly.sina;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务字段映射配置中心
 * 统一管理所有报表类型的字段映射，避免重复定义
 */
public class FinanceFieldConfig {

    // 枚举定义报表类型
    public enum ReportType {
        KEY_INDICATORS("gjzb"),   // 关键指标
        BALANCE_SHEET("fzb"),     // 资产负债表
        INCOME_STATEMENT("lrb"),  // 利润表 (预留)
        CASH_FLOW("xjl");         // 现金流量表 (预留)

        private final String sourceCode;
        ReportType(String sourceCode) {
            this.sourceCode = sourceCode;
        }
        public String getSourceCode() {
            return sourceCode;
        }
    }

    // --- 关键指标映射 (gjzb) ---
    private static final Map<String, String> KEY_INDICATORS_MAP = new HashMap<>();
    static {
        KEY_INDICATORS_MAP.put("基本每股收益", "eps");
        KEY_INDICATORS_MAP.put("稀释每股收益", "eps_diluted");
        KEY_INDICATORS_MAP.put("每股净资产", "bvps");
        KEY_INDICATORS_MAP.put("每股经营活动产生的现金流量净额", "operating_cash_flow_per_share");

        KEY_INDICATORS_MAP.put("营业总收入", "revenue");
        KEY_INDICATORS_MAP.put("营业收入", "revenue");
        KEY_INDICATORS_MAP.put("归属于母公司所有者的净利润", "net_profit");
        KEY_INDICATORS_MAP.put("净利润", "net_profit");
        KEY_INDICATORS_MAP.put("扣除非经常性损益后的净利润", "deducted_net_profit");

        KEY_INDICATORS_MAP.put("加权平均净资产收益率", "roe");
        KEY_INDICATORS_MAP.put("净资产收益率", "roe");
        KEY_INDICATORS_MAP.put("总资产收益率", "roa");
        KEY_INDICATORS_MAP.put("销售毛利率", "gross_margin");
        KEY_INDICATORS_MAP.put("销售净利率", "net_margin");

        KEY_INDICATORS_MAP.put("资产负债率", "asset_liability_ratio");
        KEY_INDICATORS_MAP.put("流动比率", "current_ratio");
        KEY_INDICATORS_MAP.put("速动比率", "quick_ratio");
    }

    // --- 资产负债表映射 (fzb) ---
    private static final Map<String, String> BALANCE_SHEET_MAP = new HashMap<>();
    static {
        // 资产类
        BALANCE_SHEET_MAP.put("货币资金", "cash_and_equivalents");
        BALANCE_SHEET_MAP.put("交易性金融资产", "trading_financial_assets");
        BALANCE_SHEET_MAP.put("应收票据及应收账款", "notes_receivable_and_accounts_receivable");
        BALANCE_SHEET_MAP.put("应收账款", "accounts_receivable");
        BALANCE_SHEET_MAP.put("预付款项", "prepayments");
        BALANCE_SHEET_MAP.put("其他应收款", "other_receivables");
        BALANCE_SHEET_MAP.put("存货", "inventory");
        BALANCE_SHEET_MAP.put("一年内到期的非流动资产", "non_current_assets_due_within_one_year");
        BALANCE_SHEET_MAP.put("其他流动资产", "other_current_assets");
        BALANCE_SHEET_MAP.put("流动资产合计", "total_current_assets");

        BALANCE_SHEET_MAP.put("长期股权投资", "long_term_equity_investment");
        BALANCE_SHEET_MAP.put("固定资产", "fixed_assets");
        BALANCE_SHEET_MAP.put("在建工程", "construction_in_progress");
        BALANCE_SHEET_MAP.put("无形资产", "intangible_assets");
        BALANCE_SHEET_MAP.put("商誉", "goodwill");
        BALANCE_SHEET_MAP.put("长期待摊费用", "long_term_deferred_expenses");
        BALANCE_SHEET_MAP.put("递延所得税资产", "deferred_tax_assets");
        BALANCE_SHEET_MAP.put("其他非流动资产", "other_non_current_assets");
        BALANCE_SHEET_MAP.put("非流动资产合计", "total_non_current_assets");

        BALANCE_SHEET_MAP.put("资产总计", "total_assets");

        // 负债类
        BALANCE_SHEET_MAP.put("短期借款", "short_term_borrowings");
        BALANCE_SHEET_MAP.put("应付票据及应付账款", "notes_payable_and_accounts_payable");
        BALANCE_SHEET_MAP.put("预收款项", "advance_receipts");
        BALANCE_SHEET_MAP.put("合同负债", "contract_liabilities");
        BALANCE_SHEET_MAP.put("应付职工薪酬", "employee_benefits_payable");
        BALANCE_SHEET_MAP.put("应交税费", "taxes_payable");
        BALANCE_SHEET_MAP.put("其他应付款", "other_payables");
        BALANCE_SHEET_MAP.put("一年内到期的非流动负债", "non_current_liabilities_due_within_one_year");
        BALANCE_SHEET_MAP.put("其他流动负债", "other_current_liabilities");
        BALANCE_SHEET_MAP.put("流动负债合计", "total_current_liabilities");

        BALANCE_SHEET_MAP.put("长期借款", "long_term_borrowings");
        BALANCE_SHEET_MAP.put("应付债券", "bonds_payable");
        BALANCE_SHEET_MAP.put("长期应付款", "long_term_payables");
        BALANCE_SHEET_MAP.put("预计负债", "provisions");
        BALANCE_SHEET_MAP.put("递延所得税负债", "deferred_tax_liabilities");
        BALANCE_SHEET_MAP.put("其他非流动负债", "other_non_current_liabilities");
        BALANCE_SHEET_MAP.put("非流动负债合计", "total_non_current_liabilities");

        BALANCE_SHEET_MAP.put("负债合计", "total_liabilities");

        // 权益类
        BALANCE_SHEET_MAP.put("实收资本(或股本)", "paid_in_capital");
        BALANCE_SHEET_MAP.put("资本公积", "capital_reserve");
        BALANCE_SHEET_MAP.put("盈余公积", "surplus_reserve");
        BALANCE_SHEET_MAP.put("未分配利润", "undistributed_profit");
        BALANCE_SHEET_MAP.put("归属于母公司所有者权益合计", "equity_attributable_to_parent");
        BALANCE_SHEET_MAP.put("少数股东权益", "minority_interest");
        BALANCE_SHEET_MAP.put("所有者权益合计", "total_equity");
    }

    /**
     * 获取指定报表类型的映射表
     * 返回不可修改的 Map，防止外部意外修改配置
     */
    public static Map<String, String> getMapping(ReportType type) {
        switch (type) {
            case KEY_INDICATORS:
                return Collections.unmodifiableMap(KEY_INDICATORS_MAP);
            case BALANCE_SHEET:
                return Collections.unmodifiableMap(BALANCE_SHEET_MAP);
            // 未来扩展其他类型
            default:
                return Collections.emptyMap();
        }
    }

    /**
     * 获取对应类型的 API Source 参数
     */
    public static String getSourceParam(ReportType type) {
        return type.getSourceCode();
    }
}
