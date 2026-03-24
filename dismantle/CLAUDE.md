# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Dismantle** - LLM context retrieval utility for reducing token costs by 60-90%.

**Key Principle:** Dismantle is NOT an LLM replacement. It's a preprocessing utility that helps agents retrieve relevant document chunks efficiently.

Two-stage API workflow:
1. **POST /api/dismantle/analyze** - Split long text → return only `id + title`
2. **POST /api/dismantle/retrieve** - User selects IDs → merge content → return `mergedText`

Agent then uses `mergedText` with their own LLM (Claude, GPT-4, etc.).

## Build & Run

```bash
# Build
mvn clean install -f dismantle/pom.xml

# Run tests
mvn test -f dismantle/pom.xml

# Start application
mvn spring-boot:run -f dismantle/pom.xml
```

## Architecture

```
com.zff.dismantle/
├── chunk/          # Text splitting strategies
│   ├── ChunkStrategy (interface)
│   ├── SemanticChunker (by paragraphs/sections)
│   └── FixedLengthChunker (by character count)
├── storage/        # In-memory session storage with TTL
│   ├── ChunkStore
│   └── AnalysisSession
├── service/        # Business logic
│   └── DismantleService (analyze + retrieve)
├── api/            # REST endpoints + DTOs + Swagger
│   ├── DismantleController (OpenAPI annotated)
│   ├── AnalyzeRequest/Response
│   ├── ChunkSelectionRequest
│   ├── MergedChunksResponse
│   ├── SessionInfoResponse
│   └── GlobalExceptionHandler
├── config/         # OpenAPI configuration
│   └── OpenApiConfig
└── metrics/        # Token usage tracking
    └── TokenMetrics
```

## Key Design Decisions

- **Token Efficiency**: Stage A response exposes ONLY `id + title`, never full content
- **Session-based**: Chunks stored server-side with TTL (60 min default)
- **No LLM Dependency**: Dismantle does NOT call any LLM - it's a pure retrieval utility
- **Semantic Chunking**: Prefers natural boundaries (headings, paragraphs)
- **Swagger First**: All endpoints fully documented with OpenAPI 3.0 annotations

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/dismantle/analyze | Stage A: Split text, return sessionId + chunk list |
| POST | /api/dismantle/retrieve | Stage B: Retrieve and merge selected chunks |
| GET | /api/dismantle/session/{id} | Get session info (titles only) |
| DELETE | /api/dismantle/session/{id} | Delete session |

## Swagger / OpenAPI

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

All endpoints include:
- Detailed descriptions with LLM/MCP integration hints
- Example requests and responses
- Schema definitions for all DTOs
- Error response documentation

## Configuration (application.yaml)

```yaml
dismantle:
  storage:
    ttl-minutes: 60
  openapi:
    server-url: http://localhost:8080
```

## Token Metrics

Tracked in every response:
- `originalTokens`: Estimated tokens in full original text
- `processedTokens`: Actual tokens in merged text (selected chunks only)
- `savings`: Percentage saved (e.g., "60%")
- `chunksSelected`: Number of chunks merged
- `totalChunks`: Total chunks available

## LLM Agent Integration Pattern

```python
# 1. Analyze document
response = post("/api/dismantle/analyze", {"text": long_text})
session_id = response["sessionId"]
chunks = response["chunks"]  # [{id, title}, ...]

# 2. Select relevant chunks (by title or LLM recommendation)
selected_ids = ["chunk_001", "chunk_003"]

# 3. Retrieve merged text
result = post("/api/dismantle/retrieve", {
    "sessionId": session_id,
    "chunkIds": selected_ids
})

merged_text = result["mergedText"]  # Use this with your LLM!

# 4. Use merged text with Claude/GPT-4/etc.
answer = llm.generate(f"Based on: {merged_text}\n\nQuestion: {query}")

# 5. (Optional) Clean up
delete(f"/api/dismantle/session/{session_id}")
```

## Documentation Files

- `readme.md` - Project overview and quick start
- `API_INTEGRATION_GUIDE.md` - Detailed API documentation for LLM/MCP agents
- `CLAUDE.md` - This file (development guide)
