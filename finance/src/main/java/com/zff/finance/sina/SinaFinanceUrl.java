package com.zff.finance.sina;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Builder(toBuilder = true) // toBuilder = true 允许从现有对象创建新的 Builder
public class SinaFinanceUrl {

    // 基础 URL 常量
    private static final String BASE_URL = "https://quotes.sina.cn/cn/api/openapi.php/CompanyFinanceService.getFinanceReport2022";

    // 必填参数
    private final String paperCode;
    private final String source;

    // 可选参数 (设置默认值需要在 Builder 中处理，或者在构造后逻辑处理)
    // 注意：@Builder 默认不会给基本类型设默认值，需配合 @Builder.Default 或自定义 Builder 逻辑
    // 0 全部 1 ，2，3，4 ：一季报,二季报
    @Builder.Default
    private final int type = 0;

    @Builder.Default
    private final int page = 1;

    @Builder.Default
    private final int num = 10; // 假设默认每页 10 条

    /**
     * 私有构造函数，强制通过 Builder 创建
     * 在此处进行最终校验
     */
    private SinaFinanceUrl(String paperCode, String source, int type, int page, int num) {
        if (paperCode == null || paperCode.trim().isEmpty()) {
            throw new IllegalArgumentException("paperCode 不能为空");
        }
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("source 不能为空");
        }
        this.paperCode = paperCode;
        this.source = source;
        this.type = type;
        this.page = page;
        this.num = num;
    }

    /**
     * 生成完整 URL 的方法
     * 也可以作为一个静态工具方法存在，但作为实例方法更符合面向对象原则
     */
    public String toUrl() {
        return new StringBuilder(BASE_URL)
                .append("?paperCode=").append(CodeUtils.toSinaPaperCode(this.paperCode))
                .append("&source=").append(this.source)
                .append("&type=").append(this.type)
                .append("&page=").append(this.page)
                .append("&num=").append(this.num)
                .toString();
    }

    /**
     * 静态工厂方法：直接返回构建好的 URL 字符串（语法糖，方便一行代码调用）
     */
    public static String createUrl(String code, String source) {
        return SinaFinanceUrl.builder()
                .paperCode(code)
                .source(source)
                .build()
                .toUrl();
    }


}