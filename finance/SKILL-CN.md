# Skill: 查询公司财务综合数据 (Finance Composite Query)

## 1. 工具概述
**名称**: `query_finance_composite`  
**域名**: `http://192.168.1.4:8080`   
**接口地址**: `POST /finance/composite/query`  
**详细且权威的接口文档**: `GET /v3/api-docs`  
**描述**: 该工具用于一站式查询上市公司的多维度财经数据。支持通过单个请求灵活组合获取**财务报表**、**实时快讯**、**未来事件**、**董秘问答**及**东方财富快讯**。  
**核心特性**:
- **动态参数组装**: 请求参数完全按需构建。查询非财报数据时，无需构造复杂的财报子对象；查询财报时，支持独立配置。
- **代码智能兼容**: `code` 字段非强制必填（但在具体业务场景中通常需要）。支持纯数字格式（如 `600519`），系统自动识别市场前缀。
- **空值安全**: 未启用的模块（开关为 `false` 或未传）在响应中返回 `null`，不报错，不影响其他数据返回。

**适用场景**: 个股全景分析、特定维度数据监控（如仅监控董秘问答）、投资研报自动生成。

## 2. 输入参数规范 (Input Schema)

调用此工具时，需构建一个 JSON 对象作为请求体 (`FinanceCompositeQuery`)。**请根据用户需要的数据类型，动态决定传入哪些字段。**

### 2.1 根对象参数 (FinanceCompositeQuery)

| 参数名 | 类型 | 必填 | 默认值 | 描述与约束                                                                                                                             | 示例值 |
| :--- | :--- | :--- | :--- |:----------------------------------------------------------------------------------------------------------------------------------| :--- |
| **`code`** | `string` | **否** | - | **股票代码**。<br>1. 启用董秘QA 或者 东方财富快讯 传入此字段以指定具体公司，其他情况不需要传递。<br>2. 查询**财报数据**时：传sinaFinanceQuery对象。<br>3. 格式推荐纯数字（`600519`），系统自动补全前缀。 | `600519` |
| **`pageSize`** | `integer` | 否 | `10` | **分页大小**。范围 1-100。仅对列表类数据（快讯、事件、QA）生效。                                                                                            | `20` |
| **`enableFastNews`** | `boolean` | 否 | `false` | **是否启用新浪财经快讯**。为 `true` 时返回 `fastNewsData`。                                                                                       | `true` |
| **`enableFutureEvent`** | `boolean` | 否 | `false` | **是否启用未来事件查询**。为 `true` 时返回 `futureEventData`。                                                                                    | `true` |
| **`enableEastNews`** | `boolean` | 否 | `false` | **是否启用东方财富快讯**。为 `true` 时返回对应数据（若有）。                                                                                              | `true` |
| **`enableQA`** | `boolean` | 否 | `false` | **是否获取董秘QA数据**。为 `true` 时返回对应数据（若有）。                                                                                              | `true` |
| **`sinaFinanceQuery`** | `object` | **否** | `null` | **财报查询专用配置**。<br>- 若用户**不需要**财报数据，**请勿传此字段**或设为 `null`。<br>- 若用户**需要**财报数据，则构造此对象（见下表）。                                           | - |

### 2.2 嵌套对象：财报配置 (SinaFinanceQuery)
**仅当用户明确需要查询财务报表时**，才构造此对象。

| 参数名 | 类型 | 必填 | 默认值 | 描述与约束 | 示例值 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`code`** | `string` | 否 | 同根对象 | **股票代码**。优先使用此处的 code；若此处缺失，则使用根对象的 `code`。支持纯数字。 | `600519` |
| **`source`** | `string` | 否 | `gjzb` | **报表类型标识**：<br>• `gjzb`: 关键指标 (默认)<br>• `fzb`: 资产负债表<br>• `lrb`: 利润表<br>• `llb`: 现金流量表 | `lrb` |
| **`type`** | `integer` | 否 | `0` | **报告期类型**：<br>• `0`: 全部 (默认)<br>• `1`: 一季报 ... `4`: 年报 | `4` |
| **`page`** | `integer` | 否 | `1` | **页码**。 | `1` |
| **`num`** | `integer` | 否 | `10` | **每页条数**。 | `10` |

## 3. 响应结构 (Output Schema)

返回对象为 `FinanceCompositeVo`。响应字段的存在与否及值取决于请求中的开关设置。

| 字段名 | 类型 | 描述 |
| :--- | :--- | :--- |
| `reports` | `Map<String, Object>` | **财务数据地图**。<br>Key: 中文指标名 (如 "营业收入")。<br>Value: 数值。<br>**注意**: 若请求中未传 `sinaFinanceQuery`，此字段为 `null`。 |
| `fastNewsData` | `Array<FastNewsVo>` | **新浪财经快讯列表**。仅当 `enableFastNews=true` 且找到数据时返回数组，否则为 `null`。 |
| `futureEventData` | `Array<FutureEventVo>` | **未来事件列表**。仅当 `enableFutureEvent=true` 时返回，否则 `null`。 |

### 数据示例

**场景 A：仅查询董秘 QA (无财报，无快讯)**
*请求*: `{"code": "002594", "enableQA": true, "pageSize": 5}`
*响应*:
```json
{
  "reports": null,
  "fastNewsData": null,
  "futureEventData": null,
  "qaData": [ ... ] // 假设返回结构
}
```

**场景 B：仅查询东方财富快讯**
*请求*: `{"code": "300750", "enableEastNews": true}`
*响应*:
```json
{
  "reports": null,
  "fastNewsData": null, 
  "eastNewsData": [ ... ] // 假设返回结构
}
```

**场景 C：仅查询利润表 (无新闻)**
*请求*: `{"sinaFinanceQuery": {"paperCode"：""，"source": "lrb","type":0}}`
*响应*:
```json
{
  "reports": {
    "营业收入": "100亿",
    "净利润": "50亿"
  },
  "fastNewsData": null,
  "futureEventData": null
}
```

## 4. 使用指南 (Usage Guide) - 动态组装策略

### 4.1 策略一：仅查询特定非财报数据 (最小化请求)
**原则**: 不需要财报时，**绝对不要**传 `sinaFinanceQuery`。只需根对象 `code` + 对应 `enable` 开关。

*   **用户指令**: "查一下比亚迪的董秘问答记录。"
*   **Agent 动作**:
    1. 识别意图：仅需 QA 数据。
    2. 提取 Code: `002594`。
    3. 构造请求: **不设** `sinaFinanceQuery`，**仅设** `enableQA=true`。
    ```json
    {
      "code": "002594",
      "pageSize": 10,
      "enableQA": true
      // sinaFinanceQuery 完全省略
    }
    ```

*   **用户指令**: "看看宁德时代最近的东方财富快讯。"
*   **Agent 动作**:
    ```json
    {
      "code": "300750",
      "enableEastNews": true
      // 其他 enable 开关默认为 false，sinaFinanceQuery 省略
    }
    ```

### 4.2 策略二：仅查询财报 (最小化请求)
**原则**: 不需要新闻时，所有 `enable` 开关保持默认 (false) 或省略，**只构造** `sinaFinanceQuery`。

*   **用户指令**: "调取贵州茅台的资产负债表。"
*   **Agent 动作**:
    1. 识别意图：仅需财报 (`fzb`)。
    2. 构造请求:
    ```json
    {
      "code": "600519", 
      "sinaFinanceQuery": {
        "source": "fzb",
        "type": 4 
      }
      // enableFastNews 等均为 false
    }
    ```

### 4.3 策略三：混合查询 (按需组合)
**原则**: 用户同时需要多种数据时，组合上述策略。

*   **用户指令**: "帮我看看腾讯控股的利润表，顺便看看有没有什么未来大事发生。"
*   **Agent 动作**:
    1. 意图：财报 (`lrb`) + 未来事件 (`enableFutureEvent`)。
    2. 构造请求:
    ```json
    {
      "code": "00700",
      "enableFutureEvent": true,
      "sinaFinanceQuery": {
        "source": "lrb"
      }
      // enableFastNews, enableQA 等均不传或为 false
    }
    ```

## 5. 最佳实践 (Best Practices)

1.  **最小必要参数原则 (Minimal Payload)**:
    - **严禁**在用户未请求财报时发送空的 `sinaFinanceQuery: {}`。要么不传该字段，要么传具体的配置。
    - **严禁**在用户只关心新闻时开启 `enableFutureEvent` 或其他无关开关。
    - 这能显著降低后端负载和响应延迟。

2.  **Code 的继承逻辑**:
    - 构造财报请求时，如果 `sinaFinanceQuery` 中没有 `code`，Agent 应确保根对象 `code` 已存在。
    - 如果用户只说了“查财报”但没给代码，且上下文中也无代码，Agent 必须先询问股票代码，因为财报查询最终必须依赖代码。

3.  **空值防御性编程**:
    - 解析响应时，对所有列表字段 (`fastNewsData`, `reports` 等) 进行 `null` 检查。
    - 逻辑示例:
      ```java
      if (response.getReports() != null) {
          renderTable(response.getReports());
      } else {
          // 静默处理或提示未查询财报
      }
      ```

4.  **代码格式容错**:
    - 无论用户输入 `600519`, `sh600519`, `SZ000001`，直接透传给后端。不要在前端做正则替换，信任后端的自动识别能力。

