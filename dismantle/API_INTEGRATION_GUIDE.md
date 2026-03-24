# Dismantle API Integration Guide

## Overview

Dismantle is a **context retrieval utility** for LLM Agents. It helps agents efficiently extract relevant information from long documents, reducing LLM token consumption by 60-90%.

**Key Design Principle:** Dismantle does NOT generate responses or call any LLM. It only retrieves and merges selected chunks for the agent to use with their own LLM (Claude, GPT-4, etc.).

---

## Quick Start

### Step 1: Analyze Document

```bash
curl -X POST http://localhost:8080/api/dismantle/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "text": "这是一篇很长的文章，包含多个章节和内容...",
    "chunkStrategy": "semantic"
  }'
```

**Response:**
```json
{
  "sessionId": "sess_abc123",
  "chunks": [
    {"id": "chunk_001", "title": "项目背景与目标"},
    {"id": "chunk_002", "title": "市场调研与分析"},
    {"id": "chunk_003", "title": "技术方案设计"},
    {"id": "chunk_004", "title": "实施计划与时间表"}
  ],
  "metrics": {
    "originalTokens": 2500,
    "processedTokens": 0,
    "savings": "100%",
    "chunksSelected": 0,
    "totalChunks": 4
  }
}
```

### Step 2: Retrieve Selected Chunks

```bash
curl -X POST http://localhost:8080/api/dismantle/retrieve \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "sess_abc123",
    "chunkIds": ["chunk_001", "chunk_003"]
  }'
```

**Response:**
```json
{
  "mergedText": "[项目背景与目标]\n本项目旨在开发一个智能数据分析系统...\n\n[技术方案设计]\n采用微服务架构，使用 Spring Boot 作为基础框架...",
  "selectedChunks": [
    {"id": "chunk_001", "title": "项目背景与目标"},
    {"id": "chunk_003", "title": "技术方案设计"}
  ],
  "metrics": {
    "originalTokens": 2500,
    "processedTokens": 1000,
    "savings": "60%",
    "chunksSelected": 2,
    "totalChunks": 4
  }
}
```

### Step 3: Use mergedText with Your LLM

```python
import anthropic

client = anthropic.Client(api_key="your-key")

merged_text = response.json()["mergedText"]
query = "项目的技术方案是什么？"

# Only 1000 tokens (selected content) vs 2500 tokens (full document)
answer = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{
        "role": "user",
        "content": f"基于以下上下文回答问题：\n\n{merged_text}\n\n问题：{query}"
    }]
)

print(answer.content[0].text)
```

---

## API Reference

### POST /api/dismantle/analyze

**Purpose:** Split long text into chunks, return session ID and chunk list.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `text` | string | Yes | The long text to analyze |
| `chunkStrategy` | string | No | `semantic` (default) or `fixed` |

**Response Fields:**
- `sessionId`: Session identifier for subsequent requests
- `chunks`: List of chunk ID + title pairs
- `metrics`: Token usage statistics

---

### POST /api/dismantle/retrieve

**Purpose:** Retrieve and merge selected chunks.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | string | Yes | Session ID from `/analyze` |
| `chunkIds` | array[string] | Yes | Selected chunk IDs to merge |

**Response Fields:**
- `mergedText`: **The complete merged text** - use this as LLM context
- `selectedChunks`: Chunks that were merged
- `metrics`: Token savings comparison

---

### GET /api/dismantle/session/{sessionId}

**Purpose:** Get session info and full chunk list.

---

### DELETE /api/dismantle/session/{sessionId}

**Purpose:** Manually delete a session.

---

## LLM Agent Integration Pattern

### Claude Code / MCP Agent Workflow

```python
async def process_document(agent, long_text, user_query):
    """
    Process a long document using Dismantle to reduce token usage.
    """
    # Step 1: Analyze document
    analyze_response = await agent.http.post(
        "http://localhost:8080/api/dismantle/analyze",
        json={"text": long_text, "chunkStrategy": "semantic"}
    )

    session_id = analyze_response["sessionId"]
    chunks = analyze_response["chunks"]

    # Step 2: Select relevant chunks based on query
    # Agent can use its own logic or let user choose
    selected_ids = await select_relevant_chunks(agent, chunks, user_query)

    # Step 3: Retrieve merged text
    retrieve_response = await agent.http.post(
        "http://localhost:8080/api/dismantle/retrieve",
        json={
            "sessionId": session_id,
            "chunkIds": selected_ids
        }
    )

    merged_text = retrieve_response["mergedText"]
    token_savings = retrieve_response["metrics"]["savings"]

    # Step 4: Use merged text with LLM
    answer = await agent.llm.generate(
        prompt=f"基于以下上下文回答问题：\n\n{merged_text}\n\n问题：{user_query}"
    )

    # Step 5: Clean up (optional, sessions auto-expire after 60 min)
    await agent.http.delete(
        f"http://localhost:8080/api/dismantle/session/{session_id}"
    )

    return {
        "answer": answer,
        "token_savings": token_savings
    }

async def select_relevant_chunks(agent, chunks, query):
    """
    Let the agent select relevant chunks based on titles.
    """
    # Option 1: Let LLM select based on titles
    chunk_list = "\n".join([f"- {c['id']}: {c['title']}" for c in chunks])

    selection_prompt = f"""
    Given the following chunk titles and a query, select the most relevant chunk IDs.
    Return only a comma-separated list of IDs.

    Chunk titles:
    {chunk_list}

    Query: {query}

    Relevant chunk IDs:
    """

    llm_response = await agent.llm.generate(prompt=selection_prompt)
    ids = [id.strip() for id in llm_response.split(",")]
    return ids
```

---

## Token Efficiency Explained

### Traditional Approach
```
Full Document (10,000 chars) → LLM → Response
Token cost: ~2,700 tokens (2,500 input + 200 output)
```

### Dismantle Approach
```
Stage 1: Document → Split → Return IDs+Titles only
         Token cost: ~50 tokens (API response, not LLM)

Stage 2: User selects 2 of 4 chunks
         Retrieve + Merge → Return merged text
         Token cost: ~1,000 tokens (only selected content)

Stage 3: merged text → Your LLM → Response
         Token cost: ~1,200 tokens (1,000 input + 200 output)

Total LLM tokens: ~1,200
Savings: 56% vs traditional approach (2,700 → 1,200)
```

### For Larger Documents
| Document Size | Traditional | Dismantle | Savings |
|---------------|-------------|-----------|---------|
| 10K chars | 2,700 tokens | 1,200 tokens | 56% |
| 50K chars | 13,000 tokens | 3,500 tokens | 73% |
| 100K chars | 25,000 tokens | 6,000 tokens | 76% |

---

## Error Handling

### Session Expired
```json
{
  "code": "INVALID_ARGUMENT",
  "message": "Session not found or expired: sess_abc123"
}
```
**Action:** Call `/analyze` again to create a new session.

### Invalid Chunk IDs
```json
{
  "code": "INVALID_ARGUMENT",
  "message": "No valid chunks found for given IDs"
}
```
**Action:** Use chunk IDs from the `/analyze` response.

---

## Best Practices

### 1. Session Management
- Sessions expire after 60 minutes by default
- Call `DELETE /session/{id}` when done to free resources
- Store sessionId for multi-turn conversations

### 2. Chunk Selection
- Let your LLM select chunks based on titles
- Or let users manually select from the chunk list
- Select only relevant chunks to maximize savings

### 3. Strategy Selection
- Use `semantic` for documents with structure (chapters, sections)
- Use `fixed` for unstructured text (logs, streams)

---

## Important Notes

### What Dismantle Does
✅ Split long text into chunks
✅ Generate concise titles for each chunk
✅ Store chunks server-side with TTL
✅ Retrieve and merge selected chunks
✅ Return merged text for agent use

### What Dismantle Does NOT Do
❌ Generate responses or answers
❌ Call any LLM for inference
❌ Replace your LLM agent
❌ Process or analyze content semantics

---

## Swagger UI

Access interactive API documentation at:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

---

## Configuration

Edit `application.yaml` to customize:

```yaml
dismantle:
  storage:
    ttl-minutes: 60           # Session expiration time
  openapi:
    server-url: http://localhost:8080
```
