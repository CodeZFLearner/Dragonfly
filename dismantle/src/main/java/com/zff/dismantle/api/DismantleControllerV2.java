package com.zff.dismantle.api;

import com.zff.dismantle.api.dto.*;
import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.metrics.TokenMetrics;
import com.zff.dismantle.service.DismantleServiceV2;
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
 * Dismantle V2 REST API - 支持渐进式披露的高级文本分块与检索工具.
 *
 * <h2>Progressive Disclosure Pattern</h2>
 *
 * <p>V2 API 实现了渐进式披露模式，允许 AI Agent 以最小的 Token 消耗逐步探索文档：
 *
 * <ol>
 *   <li><strong>Level 0 - OUTLINE</strong>: 仅返回片段 ID + 标题 (~5% tokens)</li>
 *   <li><strong>Level 1 - SUMMARY</strong>: 添加摘要和关键词 (~15% tokens)</li>
 *   <li><strong>Level 2 - EXPANDED</strong>: 添加子片段引用和元数据 (~30% tokens)</li>
 *   <li><strong>Level 3 - FULL</strong>: 返回完整内容 (100% tokens)</li>
 * </ol>
 *
 * <h2>典型使用流程</h2>
 * <pre>
 * 1. POST /api/dismantle/v2/analyze (disclosureLevel=OUTLINE)
 *    → 获取文档结构，Token 消耗最小
 *
 * 2. 根据标题选择相关片段 IDs
 *
 * 3. GET /api/dismantle/v2/session/{id}/expand/{chunkId} (targetLevel=SUMMARY)
 *    → 获取选中片段的摘要，进一步确认相关性
 *
 * 4. POST /api/dismantle/v2/retrieve
 *    → 获取最终选中片段的完整内容，用于 LLM 输入
 * </pre>
 *
 * @see DismantleServiceV2
 */
@Tag(name = "Dismantle V2 API", description = "支持渐进式披露的高级文本分块与检索 API")
@RestController
@RequestMapping("/api/dismantle/v2")
public class DismantleControllerV2 {

    private final DismantleServiceV2 dismantleServiceV2;

    public DismantleControllerV2(DismantleServiceV2 dismantleServiceV2) {
        this.dismantleServiceV2 = dismantleServiceV2;
    }

    @Operation(
            summary = "Stage A: 分析文本 (V2)",
            description = """
                    **阶段 A：分析长文本并分割为分层片段**

                    ### 新特性 (V2)
                    - **分层结构**: 支持 SECTION → SUBSECTION → PARAGRAPH 层级
                    - **渐进式披露**: 可指定返回的 detail 级别 (OUTLINE/SUMMARY/EXPANDED/FULL)
                    - **摘要生成**: 可选启用 AI 摘要生成 (enableSummary=true)
                    - **策略选择**: 支持 semantic/outline/fixed 分块策略

                    ### 披露级别说明
                    | 级别 | 内容 | Token 占比 | 用途 |
                    |------|------|-----------|------|
                    | OUTLINE | ID + 标题 | ~5% | 初始文档扫描 |
                    | SUMMARY | + 摘要 + 关键词 | ~15% | 快速相关性判断 |
                    | EXPANDED | + 子片段 + 元数据 | ~30% | 深度选择决策 |
                    | FULL | 完整内容 | 100% | 最终 LLM 输入 |

                    ### LLM Agent 使用提示
                    1. 首次调用使用 OUTLINE 级别，快速了解文档结构
                    2. 根据标题选择感兴趣的片段
                    3. 使用 /expand 接口获取摘要级别的详情
                    4. 最后使用 /retrieve 获取完整内容
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "分析成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyzeResponseV2.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "sessionId": "sess_abc123",
                                              "documentId": "doc_xyz789",
                                              "documentTitle": "项目需求文档",
                                              "disclosureLevel": "OUTLINE",
                                              "chunks": [
                                                {"id": "sec_001", "title": "项目背景", "level": "SECTION", "charCount": 2500},
                                                {"id": "sec_002", "title": "技术方案", "level": "SECTION", "charCount": 3500}
                                              ],
                                              "totalChunkCount": 15,
                                              "metrics": {
                                                "originalTokens": 2500,
                                                "processedTokens": 125,
                                                "savings": "95%",
                                                "chunksSelected": 0,
                                                "totalChunks": 15
                                              },
                                              "expandHint": "Use GET /api/dismantle/v2/session/{id}/expand/{chunkId} for details"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponseV2> analyze(
            @Valid @RequestBody AnalyzeRequestV2 request
    ) {
        AnalyzeResponseV2 response = dismantleServiceV2.analyze(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Stage B: 检索并合并片段 (V2)",
            description = """
                    **阶段 B：检索选中的片段并合并为完整文本**

                    ### 新特性 (V2)
                    - **自动扩展**: 如果选中 SECTION，可选择是否包含所有子片段
                    - **格式标记**: 可选择是否在内容前添加 [Title] 标记
                    - **Query 支持**: 可在检索时添加过滤条件

                    ### LLM Agent 使用提示
                    1. 基于之前的 OUTLINE/SUMMARY 选择，确定最终需要的片段 IDs
                    2. 调用此接口获取完整内容
                    3. 将 mergedText 作为上下文输入给 LLM
                    4. 根据 metrics 确认 Token 节省效果
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检索成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RetrieveResponseV2.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "mergedText": "[项目背景与目标]\\n本项目旨在开发...",
                                              "selectedChunks": [
                                                {"id": "sec_001", "title": "项目背景", "level": "SECTION"}
                                              ],
                                              "disclosureLevel": "FULL",
                                              "metrics": {
                                                "originalTokens": 2500,
                                                "processedTokens": 1000,
                                                "savings": "60%",
                                                "chunksSelected": 1,
                                                "totalChunks": 15
                                              },
                                              "processingInfo": "Expanded 1 section with 3 child paragraphs"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "会话不存在或片段 ID 无效")
    })
    @PostMapping("/retrieve")
    public ResponseEntity<RetrieveResponseV2> retrieve(
            @Valid @RequestBody RetrieveRequestV2 request
    ) {
        RetrieveResponseV2 response = dismantleServiceV2.retrieve(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "扩展片段详情",
            description = """
                    **获取特定片段的更详细信息**

                    ### 渐进式披露核心端点
                    此端点允许 Agent 逐步探索文档，从 OUTLINE → SUMMARY → EXPANDED → FULL

                    ### 使用场景
                    - 用户点击某个片段标题，查看详情
                    - Agent 基于摘要判断相关性
                    - 需要查看子片段列表

                    ### 披露级别升级路径
                    ```
                    OUTLINE (仅标题)
                       ↓ expand(targetLevel=SUMMARY)
                    SUMMARY (标题 + 摘要)
                       ↓ expand(targetLevel=EXPANDED)
                    EXPANDED (+ 子片段 + 元数据)
                       ↓ expand(targetLevel=FULL)
                    FULL (完整内容)
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "扩展成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChunkViewV2.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "片段 ID 无效或会话不存在")
    })
    @GetMapping("/session/{sessionId}/expand/{chunkId}")
    public ResponseEntity<ChunkViewV2> expandChunk(
            @Parameter(description = "会话 ID") @PathVariable String sessionId,
            @Parameter(description = "片段 ID") @PathVariable String chunkId,
            @Parameter(description = "目标披露级别", example = "SUMMARY")
            @RequestParam(defaultValue = "SUMMARY") DisclosureLevel targetLevel,
            @Parameter(description = "是否递归扩展子片段")
            @RequestParam(defaultValue = "false") boolean recursive
    ) {
        ChunkViewV2 chunk = dismantleServiceV2.expandChunk(sessionId, chunkId, targetLevel, recursive);
        return ResponseEntity.ok(chunk);
    }

    @Operation(
            summary = "查询文档内片段",
            description = """
                    **在文档内搜索匹配的片段**

                    ### 搜索功能
                    - **关键词匹配**: 基于关键词搜索相关片段
                    - **相关性评分**: 返回 0-1 的相关性分数
                    - **高亮显示**: 可选择在匹配内容中添加高亮标记

                    ### LLM Agent 使用提示
                    1. 用户提出问题时，提取关键词调用此接口
                    2. 根据返回的 score 排序，选择最相关的片段
                    3. 调用 /retrieve 获取完整内容
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QueryResponseV2.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "query": "技术方案",
                                              "results": [
                                                {
                                                  "chunkId": "sec_002",
                                                  "title": "技术方案设计",
                                                  "level": "SECTION",
                                                  "score": 0.95,
                                                  "snippet": "采用微服务架构...",
                                                  "charCount": 1500
                                                }
                                              ],
                                              "resultCount": 1,
                                              "metrics": {
                                                "originalTokens": 2500,
                                                "processedTokens": 375,
                                                "savings": "85%",
                                                "chunksSelected": 1,
                                                "totalChunks": 15
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "会话不存在")
    })
    @PostMapping("/query")
    public ResponseEntity<QueryResponseV2> query(
            @Valid @RequestBody QueryRequestV2 request
    ) {
        QueryResponseV2 response = dismantleServiceV2.query(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "获取会话信息 (V2)",
            description = """
                    **获取会话详情，包含所有根级别片段**

                    ### 与 V1 的区别
                    - 返回分层结构信息
                    - 支持指定披露级别
                    - 包含文档元数据
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyzeResponseV2.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "会话不存在")
    })
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<AnalyzeResponseV2> getSessionInfo(
            @Parameter(description = "会话 ID") @PathVariable String sessionId,
            @Parameter(description = "披露级别")
            @RequestParam(defaultValue = "OUTLINE") DisclosureLevel disclosureLevel
    ) {
        AnalyzeResponseV2 response = dismantleServiceV2.getSessionInfo(sessionId, disclosureLevel);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "删除会话",
            description = """
                    **手动删除会话，释放存储空间**

                    ### 注意
                    - 会话默认 60 分钟后自动过期
                    - 处理完成后可主动调用此接口清理
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "会话不存在")
    })
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "会话 ID") @PathVariable String sessionId
    ) {
        dismantleServiceV2.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
