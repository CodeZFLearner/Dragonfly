package com.zff.dismantle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger 配置
 */
@Configuration
public class OpenApiConfig {

    @Value("${dismantle.openapi.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI dismantleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dismantle API")
                        .description("""
                                ## 智能文本分块与合并工具

                                Dismantle 是一个专为 LLM Agent 设计的上下文检索工具。
                                通过两阶段工作流，帮助 Agent 精准定位并提取文档中的核心信息，显著减少 LLM Token 消耗。

                                ### 核心特性
                                - **Token 效率**: 仅检索用户选中的内容，节省 60%-90% Token
                                - **会话管理**: 自动过期清理，无需手动管理
                                - **灵活分块**: 支持语义分割和固定长度分割
                                - **无 LLM 依赖**: 本工具 NOT 调用任何 LLM，仅做检索和合并

                                ### LLM Agent 典型使用流程

                                ```
                                1. 调用 POST /api/dismantle/analyze
                                   → 输入：长文本（如 100K 字符文档）
                                   → 输出：sessionId + chunk 列表（ID + 标题）

                                2. Agent 根据标题选择相关 chunk IDs
                                   → 例如：["chunk_001", "chunk_003", "chunk_005"]

                                3. 调用 POST /api/dismantle/retrieve
                                   → 输入：sessionId + selected chunkIds
                                   → 输出：mergedText（合并后的完整文本）

                                4. Agent 将 mergedText 作为上下文输入给 LLM
                                   → 使用 Claude、GPT-4 等生成回答
                                   → 仅消耗选中内容的 Token（而非全文）

                                5. （可选）调用 DELETE /api/dismantle/session/{id} 清理
                                ```

                                ### Token 节省示例

                                | 场景 | 原文档 | 选中内容 | 节省 |
                                |------|--------|----------|------|
                                | 10K 字符文档 | 2500 tokens | 1000 tokens | 60% |
                                | 50K 字符文档 | 12500 tokens | 3000 tokens | 76% |
                                | 100K 字符文档 | 25000 tokens | 5000 tokens | 80% |

                                ### 重要说明

                                **Dismantle NOT 是 LLM 替代品！**

                                它是一个预处理工具，帮助 Agent：
                                - 理解长文档结构（通过 chunk 标题）
                                - 精准定位相关信息（通过选择 chunk IDs）
                                - 减少 LLM 输入 Token（仅输入选中内容）

                                Agent 仍需使用自己的 LLM（Claude、GPT-4 等）进行推理和回答。

                                ### 相关链接
                                - Swagger UI: /swagger-ui.html
                                - OpenAPI JSON: /v3/api-docs
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Dismantle Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("本地开发环境"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地服务器")
                ));
    }
}
