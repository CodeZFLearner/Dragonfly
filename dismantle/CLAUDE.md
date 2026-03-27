# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Dismantle** - LLM context retrieval utility with Progressive Disclosure for reducing token costs by 60-96%.

**Key Principle:** Dismantle is NOT an LLM replacement. It's a preprocessing utility that helps agents retrieve relevant document chunks efficiently.

**V2 Feature:** Progressive Disclosure - reveal document content incrementally to minimize token usage.

### Two-stage API workflow:
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

### V2 Architecture (Progressive Disclosure)

```
com.zff.dismantle/
├── core/                      # NEW: Core domain model
│   ├── Document               # Aggregated root with hierarchical chunks
│   ├── HierarchicalChunk      # Chunk with parent/child relationships
│   ├── ChunkLevel             # SECTION, SUBSECTION, PARAGRAPH levels
│   ├── ChunkMetadata          # Extensible metadata container
│   └── DisclosureLevel        # OUTLINE, SUMMARY, EXPANDED, FULL
│
├── chunk/                     # Text splitting strategies
│   ├── ChunkStrategy          # Base interface
│   ├── HierarchicalChunkStrategy # V2 hierarchical interface
│   ├── SemanticChunker        # Enhanced: supports hierarchical output
│   ├── OutlineChunker         # NEW: extract only document structure
│   └── FixedLengthChunker     # Fixed-size chunking
│
├── enrichment/                # NEW: Content enrichment layer
│   ├── TitleGenerator         # Title generation interface
│   ├── SummaryGenerator       # Summary generation interface
│   ├── RuleBasedTitleGenerator # Fast rule-based titles
│   ├── LlmTitleGenerator      # LLM-generated titles (Ollama)
│   ├── RuleBasedSummaryGenerator # Rule-based summaries
│   └── LlmSummaryGenerator    # LLM-generated summaries
│
├── retrieval/                 # NEW: Search and retrieval
│   ├── Retriever              # Retrieval strategy interface
│   ├── KeywordRetriever       # BM25-style keyword matching
│   └── HybridRetriever        # Combined keyword + semantic (future)
│
├── storage/                   # Session storage
│   ├── ChunkStore             # In-memory storage with TTL
│   └── AnalysisSession        # Enhanced: supports V2 document references
│
├── service/                   # Business logic
│   ├── DismantleService       # V1 service (backward compatible)
│   └── DismantleServiceV2     # NEW: Progressive disclosure service
│
├── api/                       # REST endpoints
│   ├── dto/                   # NEW: V2 DTOs
│   │   ├── AnalyzeRequestV2
│   │   ├── AnalyzeResponseV2
│   │   ├── RetrieveRequestV2
│   │   ├── RetrieveResponseV2
│   │   ├── QueryRequestV2
│   │   ├── QueryResponseV2
│   │   ├── ChunkViewV2
│   │   └── ExpandRequestV2
│   ├── DismantleController    # V1 endpoints (backward compatible)
│   ├── DismantleControllerV2  # NEW: V2 endpoints
│   └── exception/             # NEW: Centralized exception handling
│       └── GlobalExceptionHandler
│
├── config/                    # Configuration
│   ├── OpenApiConfig          # Swagger/OpenAPI configuration
│   └── DismantleProperties    # NEW: YAML configuration binding
│
└── metrics/                   # Token usage tracking
    └── TokenMetrics
```

## Key Design Decisions

- **Progressive Disclosure**: V2 API reveals content incrementally (OUTLINE → SUMMARY → EXPANDED → FULL)
- **Token Efficiency**: OUTLINE level uses only ~5% tokens vs full document
- **Hierarchical Chunking**: Supports SECTION → SUBSECTION → PARAGRAPH structure
- **Pluggable Strategies**: Easy to add new chunking, title, and retrieval strategies
- **Backward Compatible**: V1 API continues to work for existing clients
- **No LLM Dependency**: Core functionality works without LLM; LLM features are optional enhancements
- **Swagger First**: All endpoints fully documented with OpenAPI 3.0 annotations

## API Endpoints

### V1 API (Backward Compatible)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/dismantle/analyze | Stage A: Split text, return sessionId + chunk list |
| POST | /api/dismantle/retrieve | Stage B: Retrieve and merge selected chunks |
| GET | /api/dismantle/session/{id} | Get session info (titles only) |
| DELETE | /api/dismantle/session/{id} | Delete session |

### V2 API (Progressive Disclosure)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/dismantle/v2/analyze | Analyze with disclosure level (OUTLINE/SUMMARY/EXPANDED/FULL) |
| POST | /api/dismantle/v2/retrieve | Retrieve with auto-expand children support |
| GET | /api/dismantle/v2/session/{id}/expand/{chunkId} | Expand specific chunk to higher detail level |
| POST | /api/dismantle/v2/query | Search within document by keywords |
| GET | /api/dismantle/v2/session/{id} | Get session info with disclosure level |
| DELETE | /api/dismantle/v2/session/{id} | Delete session |

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
    backend: memory          # memory (redis for future)
  chunking:
    default-strategy: semantic  # semantic, outline, fixed
    semantic:
      min-section-length: 50
      enable-hierarchical: true
  enrichment:
    title-generator: rule-based   # rule-based or llm
    summary-generator: rule-based # rule-based or llm
    llm:
      provider: ollama
      endpoint: http://localhost:11434
      model: qwen2.5:7b
  retrieval:
    default-strategy: keyword   # keyword, semantic, hybrid
    max-results: 10
    min-score: 0.3
  openapi:
    server-url: http://localhost:8080
```

## Token Metrics

Tracked in every response:
- `originalTokens`: Estimated tokens in full original text
- `processedTokens`: Actual tokens returned (varies by disclosure level)
- `savings`: Percentage saved (e.g., "95%" for OUTLINE level)
- `chunksSelected`: Number of chunks merged
- `totalChunks`: Total chunks available

### Token Savings by Disclosure Level

| Level | Content | Typical Token Usage |
|-------|---------|---------------------|
| OUTLINE | ID + Title only | ~5% of full document |
| SUMMARY | + Summary + Keywords | ~15% of full document |
| EXPANDED | + Children + Metadata | ~30% of full document |
| FULL | Complete content | 100% of selected chunks |

## LLM Agent Integration Pattern

### V2 Progressive Disclosure Pattern (Recommended)

```python
# 1. Analyze document at OUTLINE level (minimal tokens)
response = post("/api/dismantle/v2/analyze", {
    "text": long_text,
    "disclosureLevel": "OUTLINE"
})
session_id = response["sessionId"]
chunks = response["chunks"]  # Only id + title, ~5% tokens

# 2. Agent selects relevant chunks by title
selected_ids = llm.select_by_titles(chunks, query)

# 3. Expand to SUMMARY level for relevance confirmation
for chunk_id in selected_ids:
    expanded = get(f"/api/dismantle/v2/session/{session_id}/expand/{chunk_id}",
                   params={"targetLevel": "SUMMARY"})
    if llm.is_relevant(expanded["summary"], query):
        final_ids.append(chunk_id)

# 4. Retrieve full content of confirmed chunks
result = post("/api/dismantle/v2/retrieve", {
    "sessionId": session_id,
    "chunkIds": final_ids,
    "includeChildren": True
})

merged_text = result["mergedText"]  # Use this with your LLM!

# 5. Use merged text with Claude/GPT-4/etc.
answer = llm.generate(f"Based on: {merged_text}\n\nQuestion: {query}")

# 6. (Optional) Clean up
delete(f"/api/dismantle/v2/session/{session_id}")
```

## Documentation Files

- `readme.md` - Project overview and quick start
- `API_INTEGRATION_GUIDE.md` - Detailed API documentation for LLM/MCP agents
- `CLAUDE.md` - This file (development guide)
