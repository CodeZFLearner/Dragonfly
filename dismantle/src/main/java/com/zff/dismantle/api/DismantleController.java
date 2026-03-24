package com.zff.dismantle.api;

import com.zff.dismantle.service.DismantleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dismantle REST API - 智能文本分块与合并工具
 *
 * <h2>两阶段工作流程</h2>
 *
 * <h3>Stage A: 分析 (Analyze)</h3>
 * <pre>
 * 1. 提交长文本 → 系统分割为多个片段
 * 2. 为每个片段生成标题和摘要（内部存储）
 * 3. 返回 sessionId + 片段列表（仅 ID + 标题，不暴露全文）
 * </pre>
 *
 * <h3>Stage B: 检索合并 (Retrieve)</h3>
 * <pre>
 * 1. 用户根据标题选择相关片段 ID
 * 2. 系统检索选中片段的完整内容
 * 3. 合并为完整文本返回
 * 4. Agent 使用合并后的文本 + 自己的 LLM 进行推理
 * </pre>
 *
 * <h2>Token 节省原理</h2>
 * Stage A 响应仅包含 ID + 标题（约 50 tokens），而非全文（可能 2500+ tokens）。
 * 用户基于标题选择相关片段，系统仅加载选中内容，显著减少 Token 消耗。
 *
 * <h2>LLM Agent 使用模式</h2>
 * <pre>
 * 1. 调用 /analyze 分割文档 → 获得 chunk 列表
 * 2. 根据标题选择相关 chunkIds
 * 3. 调用 /retrieve 获取合并文本
 * 4. 将合并文本作为上下文输入给 LLM（如 Claude、GPT-4）
 * 5. LLM 基于精准上下文回答问题
 * </pre>
 */
@Tag(name = "Dismantle API", description = "智能文本分块与合并 API - 两阶段工作流，优化 LLM Token 使用")
@RestController
@RequestMapping("/api/dismantle")
public class DismantleController {

    private final DismantleService dismantleService;

    public DismantleController(DismantleService dismantleService) {
        this.dismantleService = dismantleService;
    }

    @Operation(
            summary = "Stage A: 分析文本",
            description = """
                    **阶段 A：分析长文本并分割为片段**

                    ### 工作流程
                    1. 接收长文本，按语义或固定长度分割为多个片段
                    2. 为每个片段生成标题和摘要（存储于服务端，不返回）
                    3. 创建会话（session），60 分钟后自动过期
                    4. 返回 sessionId 和片段列表（仅 ID + 标题）

                    ### 为什么只返回 ID + 标题？
                    - **Token 效率**：避免在用户选择前暴露全文，减少 90%+ 初始 Token 消耗
                    - **用户决策**：基于简洁标题选择相关片段，避免信息过载

                    ### LLM/MCP 集成提示
                    此接口设计专为 LLM Agent 优化：
                    - Agent 可调用此接口获取文档结构
                    - 根据 chunk 标题选择相关片段
                    - 再调用 `/retrieve` 接口获取合并文本

                    ### 分块策略
                    - `semantic`: 按段落/章节分割（推荐，适合有结构的文档）
                    - `fixed`: 按固定长度分割（适合无结构文本）
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "分析成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyzeResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "sessionId": "sess_abc123",
                                              "chunks": [
                                                {"id": "chunk_001", "title": "项目背景与目标"},
                                                {"id": "chunk_002", "title": "市场调研与分析"},
                                                {"id": "chunk_003", "title": "技术方案设计"}
                                              ],
                                              "metrics": {
                                                "originalTokens": 2500,
                                                "processedTokens": 0,
                                                "savings": "100%",
                                                "chunksSelected": 0,
                                                "totalChunks": 3
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "请求参数错误",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "VALIDATION_ERROR",
                                              "message": "Invalid request parameters",
                                              "fieldErrors": {
                                                "text": "Text is required"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(
            @Valid @RequestBody AnalyzeRequest request
    ) {
        AnalyzeResponse response = dismantleService.analyze(
                request.getText(),
                request.getChunkStrategy()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Stage B: 检索并合并片段",
            description = """
                    **阶段 B：检索选中的片段并合并为完整文本**

                    ### 工作流程
                    1. 用户提供 sessionId 和选中的 chunkIds 列表
                    2. 系统从存储中检索选中片段的**完整内容**
                    3. 合并所有选中内容为单一文本
                    4. 返回合并文本（不含任何 LLM 处理）

                    ### LLM/MCP 集成提示
                    Agent 典型使用模式：
                    ```
                    1. 调用 /analyze 获取文档结构和 chunk 列表
                    2. 根据 chunk 标题选择相关 IDs（如：chunk_001, chunk_003）
                    3. 调用 /retrieve 获取合并文本
                    4. 将 mergedText 作为上下文输入给你的 LLM（Claude/GPT-4等）
                    5. LLM 基于精准上下文回答问题
                    ```

                    ### Token 计算
                    响应中 `metrics` 字段显示：
                    - `originalTokens`: 原文档总 Token 数
                    - `processedTokens`: 合并后的 Token 数（仅选中内容）
                    - `savings`: 节省百分比（如 "40%" 表示节省 40% Token）

                    ### 重要说明
                    **此接口 NOT 调用 LLM 生成回答！**
                    它只是一个检索和合并工具，返回的 mergedText 供 agent 自行使用。
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检索成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MergedChunksResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "mergedText": "[项目背景与目标]\n本项目旨在开发一个智能数据分析系统...\n\n[技术方案设计]\n采用微服务架构...",
                                              "selectedChunks": [
                                                {"id": "chunk_001", "title": "项目背景与目标"},
                                                {"id": "chunk_003", "title": "技术方案设计"}
                                              ],
                                              "metrics": {
                                                "originalTokens": 2500,
                                                "processedTokens": 1000,
                                                "savings": "60%",
                                                "chunksSelected": 2,
                                                "totalChunks": 3
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "会话不存在或已过期/片段 ID 无效",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "INVALID_ARGUMENT",
                                              "message": "Session not found or expired: sess_abc123"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/retrieve")
    public ResponseEntity<MergedChunksResponse> retrieveChunks(
            @Valid @RequestBody ChunkSelectionRequest request
    ) {
        MergedChunksResponse response = dismantleService.retrieveChunks(
                request.getSessionId(),
                request.getChunkIds()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "获取会话信息",
            description = """
                    获取指定会话的详细信息，包括：
                    - 会话创建时间和过期时间
                    - 所有片段的 ID 和标题列表（不包含全文内容）

                    ### 用途
                    - 检查会话是否仍然有效
                    - 获取完整的 chunk 列表供用户选择

                    ### LLM/MCP 集成提示
                    Agent 可在以下场景使用：
                    - 用户询问"文档有哪些部分？"
                    - 需要重新展示 chunk 列表供选择
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SessionInfoResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "会话不存在或已过期")
    })
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionInfoResponse> getSessionInfo(
            @Parameter(description = "会话 ID，由 /analyze 接口返回",
                       example = "sess_abc123")
            @PathVariable String sessionId
    ) {
        Map<String, Object> info = dismantleService.getSessionInfo(sessionId);

        SessionInfoResponse response = SessionInfoResponse.builder()
                .sessionId((String) info.get("sessionId"))
                .createdAt(java.time.Instant.parse((String) info.get("createdAt")))
                .expiresAt(java.time.Instant.parse((String) info.get("expiresAt")))
                .chunkCount((Integer) info.get("chunkCount"))
                .chunks((java.util.List<com.zff.dismantle.chunk.ChunkView>) info.get("chunks"))
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "删除会话",
            description = """
                    手动删除会话，释放存储空间。

                    ### 注意
                    - 会话默认 60 分钟后自动过期
                    - 处理完成后可主动调用此接口清理

                    ### LLM/MCP 集成提示
                    Agent 应在以下时机调用：
                    - 用户明确完成当前任务
                    - 会话不再需要时主动清理
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "会话不存在")
    })
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "会话 ID")
            @PathVariable String sessionId
    ) {
        dismantleService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
