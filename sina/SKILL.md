# Skill: Finance Data Query

## 1. Tool Overview
**Name**: `query_finance_data`  
**HOST**: `http://192.168.1.4:8080`   
**Endpoint**: `GET /sina/finance/data`  
**Description**: Retrieves structured financial statement data for listed companies from the Sina Finance data source. Supports querying key financial indicators, balance sheets, income statements, and cash flow statements. Users can specify stock codes, report types, and pagination parameters to retrieve precise datasets.  
**Use Cases**: Financial data analysis, fundamental stock research, automated earnings monitoring, and comparative financial metric analysis.


## 2. Input Parameter Specification

When invoking this tool, construct a query object (`SinaFinanceQueryDTO`). All parameters are passed as URL Query parameters.

### Core Parameters

| Parameter | Type | Required | Default | Description & Constraints | Example |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`code`** | `string` | **Yes** | - | **Stock Code**. Usually includes a market prefix (e.g., `sh` for Shanghai, `sz` for Shenzhen, `hk` for Hong Kong). | `sh600519`, `sz000001` |
| **`source`** | `string` | No | `gjzb` | **Data Source Identifier**. Determines the type of financial report.• `gjzb`: Key Indicators (Default)• `fzb`: Balance Sheet• `lrb`: Income Statement• `llb`: Cash Flow Statement | `lrb` (Income Statement) |
| **`type`** | `integer` | No | `0` | **Report Period Type**.• `0`: All Periods (Default)• `1`: Q1 Report• `2`: Interim Report (H1)• `3`: Q3 Report• `4`: Annual Report | `4` (Annual Report only) |
| **`page`** | `integer` | No | `1` | **Page Number**. Minimum value is 1. Used for pagination. | `2` |
| **`num`** | `integer` | No | `10` | **Items Per Page**. Range: 1–100. Recommended: 20–50 to balance performance and data volume. | `20` |

## 3. Usage Guide

### 3.1 Basic Query: Retrieve Key Indicators
Use this when the user wants a general financial overview of a stock. Default parameters (`source=gjzb`, `type=0`) are sufficient.

*   **User Instruction**: "Get financial data for Kweichow Moutai."
*   **Tool Invocation**:
    ```http
    GET /sina/finance/data?code=sh600519&source=gjzb&type=0&page=1&num=10
    ```

### 3.2 Advanced Query: Specific Financial Statements
Use this when the user needs to analyze specific aspects like profitability or assets. Explicitly set the `source` parameter.

*   **User Instruction**: "Show me Tencent's income statement for last year."
*   **Tool Invocation**:
    ```http
    GET /sina/finance/data?code=hk00700&source=lrb&type=4&page=1&num=20
    ```
    *(Note: Ensure the stock code format matches the data source requirements. For HK stocks, prefixes like `hk` may be required.)*

### 3.3 Pagination Strategy
Financial history can be extensive.
*   **Strategy**: Start with `page=1`. If the number of returned items equals the `num` parameter, automatically increment `page` and fetch the next batch until fewer items are returned or a safety limit (e.g., 5 pages) is reached.
*   **User Instruction**: "List all annual reports for BYD."
*   **Logic**:
    1. Call with `type=4` (Annual), `page=1`, `num=50`.
    2. If 50 items returned, call `page=2`.
    3. Aggregate and summarize results.

## 4. Best Practices

*   **Concurrent Execution**: For comparison requests (e.g., "Compare Moutai and Wuliangye"), invoke the tool concurrently for each stock code to minimize latency, then perform a横向 (horizontal) analysis.
*   **Context Retention**: Maintain the `code` in conversation context. If a user asks, "How about its cash flow?", reuse the stored `code` and switch `source` to `llb` without asking for the stock name again.
*   **Code Normalization**: If a user provides a code without a market prefix (e.g., "600519"), attempt to infer the prefix (e.g., `sh600519` for 6-digit codes starting with 6) before calling the API.
*   **Error Handling**:
    *   **Missing Code**: Prompt the user: "Please provide the stock code or company name."
    *   **Empty Result**: Inform the user: "No data found. Please verify the stock code or try a different report type (e.g., switch from 'Key Indicators' to 'Income Statement')."

## 5. Example Dialogue Flow

**User**: "Check CATL's 2023 annual report data."
**Agent (Thought)**: User needs annual data (`type=4`) for CATL (Code: `sz300750`). I will start by fetching Key Indicators for a summary.
**Tool Call**: `query_finance_data(code="sz300750", source="gjzb", type=4, page=1, num=10)`
**Agent (Response)**:
"Here are the key financial indicators for CATL (sz300750) for the 2023 fiscal year:

| Metric | Value |
| :--- | :--- |
| Earnings Per Share (EPS) | ... |
| Net Profit | ... |
| ROE | ... |

Would you like to see the detailed Income Statement or Balance Sheet?"

**User**: "Yes, show me the Income Statement."
**Agent (Thought)**: Reuse `code="sz300750"` and `type=4`. Change `source` to `lrb`.
**Tool Call**: `query_finance_data(code="sz300750", source="lrb", type=4, page=1, num=20)`
**Agent (Response)**:
"Here is the detailed Income Statement for CATL in 2023:

| Item | Amount (CNY) |
| :--- | :--- |
| Revenue | ... |
| Operating Profit | ... |
| Net Profit | ... |
"