package com.zff.dismantle.service;

import com.zff.dismantle.api.dto.*;
import com.zff.dismantle.chunk.*;
import com.zff.dismantle.core.*;
import com.zff.dismantle.enrichment.*;
import com.zff.dismantle.metrics.TokenMetrics;
import com.zff.dismantle.storage.AnalysisSession;
import com.zff.dismantle.storage.ChunkStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service V2 for Dismantle operations with progressive disclosure support.
 *
 * <p>This service implements the Progressive Disclosure pattern:
 * <ul>
 *   <li>Level 0 (OUTLINE): Return only structure (titles) - minimal tokens</li>
 *   <li>Level 1 (SUMMARY): Add summaries and keywords - moderate tokens</li>
 *   <li>Level 2 (EXPANDED): Add child references and metadata - more tokens</li>
 *   <li>Level 3 (FULL): Return complete content - all tokens</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <pre>
 * Document (analyzed)
 * └── HierarchicalChunk tree
 *     ├── SECTION level
 *     │   └── SUBSECTION level
 *     │       └── PARAGRAPH level
 * </pre>
 */
@Slf4j
@Service
public class DismantleServiceV2 {

    private final ChunkStore chunkStore;
    private final SemanticChunker semanticChunker;
    private final OutlineChunker outlineChunker;
    private final FixedLengthChunker fixedChunker;
    private final RuleBasedTitleGenerator titleGenerator;
    private final RuleBasedSummaryGenerator summaryGenerator;

    // Document storage (in-memory, will be abstracted in Phase 6)
    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();

    public DismantleServiceV2(
            ChunkStore chunkStore,
            SemanticChunker semanticChunker,
            OutlineChunker outlineChunker,
            FixedLengthChunker fixedChunker,
            RuleBasedTitleGenerator titleGenerator,
            RuleBasedSummaryGenerator summaryGenerator
    ) {
        this.chunkStore = chunkStore;
        this.semanticChunker = semanticChunker;
        this.outlineChunker = outlineChunker;
        this.fixedChunker = fixedChunker;
        this.titleGenerator = titleGenerator;
        this.summaryGenerator = summaryGenerator;
    }

    /**
     * Stage A: Analyze text with progressive disclosure support.
     */
    public AnalyzeResponseV2 analyze(AnalyzeRequestV2 request) {
        // Select chunking strategy
        HierarchicalChunkStrategy strategy = selectStrategy(request.getChunkStrategy());

        // Analyze and chunk the document
        Document document = strategy.analyze(request.getText(), request.getDisclosureLevel());

        // Set document title if provided
        if (request.getDocumentTitle() != null && !request.getDocumentTitle().isBlank()) {
            document.setTitle(request.getDocumentTitle());
            document.putMetadata("userTitle", request.getDocumentTitle());
        }

        // Generate document ID
        String documentId = UUID.randomUUID().toString();
        document.setId(documentId);

        // Store document
        documentStore.put(documentId, document);

        // Create session and store reference
        String sessionId = UUID.randomUUID().toString();
        AnalysisSession session = chunkStore.createSession(sessionId, request.getText());
        session.setDocumentId(documentId);

        // Also store document in session metadata for backward compatibility
        storeDocumentInSession(session, document);

        // Build response
        List<ChunkViewV2> chunkViews = buildChunkViews(
                document.getRootChunks(),
                request.getDisclosureLevel(),
                document
        );

        int originalTokens = request.getText().length() / 4;
        int processedTokens = estimateTokensForDisclosure(chunkViews, request.getDisclosureLevel());

        return AnalyzeResponseV2.builder()
                .sessionId(sessionId)
                .documentId(documentId)
                .documentTitle(document.getTitle())
                .disclosureLevel(request.getDisclosureLevel())
                .chunks(chunkViews)
                .totalChunkCount(document.getChunkCount())
                .metrics(TokenMetrics.of(originalTokens, processedTokens, 0, document.getChunkCount()))
                .expandHint("Use GET /api/dismantle/v2/session/{id}/expand/{chunkId} for details")
                .build();
    }

    /**
     * Stage B: Retrieve and merge selected chunks.
     */
    public RetrieveResponseV2 retrieve(RetrieveRequestV2 request) {
        // Get session
        AnalysisSession session = chunkStore.getSession(request.getSessionId());
        if (session == null) {
            throw new IllegalArgumentException("Session not found or expired: " + request.getSessionId());
        }

        // Get document
        String documentId = session.getDocumentId();
        if (documentId == null) {
            // Fallback to V1 behavior
            return retrieveV1(request, session);
        }

        Document document = documentStore.get(documentId);
        if (document == null) {
            // Try to reload from session
            document = loadDocumentFromSession(session);
            if (document == null) {
                throw new IllegalArgumentException("Document not found for session");
            }
        }

        // Retrieve selected chunks
        List<HierarchicalChunk> selectedChunks = new ArrayList<>();
        int expandedCount = 0;

        for (String chunkId : request.getChunkIds()) {
            HierarchicalChunk chunk = document.getChunk(chunkId);
            if (chunk == null) {
                log.warn("Chunk {} not found in document {}", chunkId, documentId);
                continue;
            }

            // Load full content
            loadChunkContent(chunk, session);

            // Expand children if requested
            if (request.isIncludeChildren() && chunk.hasChildren()) {
                for (String childId : chunk.getChildIds()) {
                    HierarchicalChunk childChunk = document.getChunk(childId);
                    if (childChunk != null) {
                        loadChunkContent(childChunk, session);
                        selectedChunks.add(childChunk);
                        expandedCount++;
                    }
                }
            } else {
                selectedChunks.add(chunk);
            }
        }

        // Merge chunks
        StringBuilder mergedText = new StringBuilder();
        for (HierarchicalChunk chunk : selectedChunks) {
            if (request.isAddFormatMarkers()) {
                mergedText.append("[").append(chunk.getTitle()).append("]\n");
            }
            if (chunk.hasContent()) {
                mergedText.append(chunk.getContent()).append("\n\n");
            }
        }

        String mergedTextStr = mergedText.toString().trim();

        // Build selected chunk views (FULL level)
        List<ChunkViewV2> selectedChunkViews = selectedChunks.stream()
                .map(this::toChunkViewV2Full)
                .toList();

        // Calculate metrics
        int originalTokens = session.getOriginalText().length() / 4;
        int mergedTokens = mergedTextStr.length() / 4;

        String processingInfo = expandedCount > 0
                ? "Expanded " + request.getChunkIds().size() + " sections with " + expandedCount + " child chunks"
                : "Retrieved " + selectedChunks.size() + " chunks";

        return RetrieveResponseV2.builder()
                .mergedText(mergedTextStr)
                .selectedChunks(selectedChunkViews)
                .disclosureLevel(DisclosureLevel.FULL)
                .metrics(TokenMetrics.of(originalTokens, mergedTokens, selectedChunks.size(), document.getChunkCount()))
                .processingInfo(processingInfo)
                .build();
    }

    /**
     * Expand a specific chunk to a higher disclosure level.
     */
    public ChunkViewV2 expandChunk(
            String sessionId,
            String chunkId,
            DisclosureLevel targetLevel,
            boolean recursive
    ) {
        // Get session
        AnalysisSession session = chunkStore.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found or expired: " + sessionId);
        }

        // Get document
        String documentId = session.getDocumentId();
        Document document = documentId != null ? documentStore.get(documentId) : null;

        if (document == null) {
            // Fallback to V1
            throw new IllegalArgumentException("Document not found. Please re-analyze the document.");
        }

        // Get chunk
        HierarchicalChunk chunk = document.getChunk(chunkId);
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk not found: " + chunkId);
        }

        // Load content based on target level
        if (targetLevel == DisclosureLevel.FULL) {
            loadChunkContent(chunk, session);
        }

        // Load summary if needed
        if (targetLevel.includes(DisclosureLevel.SUMMARY) && !chunk.hasSummary()) {
            String content = chunk.getContent();
            if (content == null && session.hasV1Chunks()) {
                // Load from V1 chunks
                com.zff.dismantle.chunk.Chunk v1Chunk = session.getV1Chunk(chunkId);
                if (v1Chunk != null) {
                    content = v1Chunk.getContent();
                }
            }
            if (content != null) {
                Summary summary = summaryGenerator.generate(content, SummaryLevel.BRIEF);
                chunk.setSummary(summary.getText());
            }
        }

        // Build view based on target level
        return switch (targetLevel) {
            case OUTLINE -> toChunkViewV2(chunk, document, DisclosureLevel.OUTLINE);
            case SUMMARY -> toChunkViewV2(chunk, document, DisclosureLevel.SUMMARY);
            case EXPANDED -> toChunkViewV2(chunk, document, DisclosureLevel.EXPANDED);
            case FULL -> toChunkViewV2(chunk, document, DisclosureLevel.FULL);
        };
    }

    /**
     * Query within document for matching chunks.
     */
    public QueryResponseV2 query(QueryRequestV2 request) {
        // Get session
        AnalysisSession session = chunkStore.getSession(request.getSessionId());
        if (session == null) {
            throw new IllegalArgumentException("Session not found or expired: " + request.getSessionId());
        }

        // Get document
        String documentId = session.getDocumentId();
        Document document = documentId != null ? documentStore.get(documentId) : null;

        if (document == null) {
            // Fallback to simple keyword search in V1 chunks
            return queryV1(request, session);
        }

        // Perform keyword-based search
        List<QueryResponseV2.QueryResultView> results = new ArrayList<>();
        String query = request.getQuery().toLowerCase();

        for (HierarchicalChunk chunk : document.getChunks().values()) {
            // Load content for matching
            loadChunkContent(chunk, session);

            if (!chunk.hasContent()) {
                continue;
            }

            String content = chunk.getContent().toLowerCase();
            String title = chunk.getTitle().toLowerCase();

            // Calculate score
            double score = calculateKeywordScore(query, title, content);

            if (score >= request.getMinScore()) {
                String snippet = extractSnippet(chunk.getContent(), query);
                String highlightedSnippet = request.isHighlight()
                        ? highlightMatches(snippet, query)
                        : snippet;

                results.add(QueryResponseV2.QueryResultView.builder()
                        .chunkId(chunk.getId())
                        .title(chunk.getTitle())
                        .level(chunk.getLevel() != null ? chunk.getLevel().name() : "UNKNOWN")
                        .score(score)
                        .snippet(snippet)
                        .highlightedSnippet(highlightedSnippet)
                        .charCount(chunk.getCharCount())
                        .build());
            }
        }

        // Sort by score and limit results
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (results.size() > request.getMaxResults()) {
            results = results.subList(0, request.getMaxResults());
        }

        // Calculate metrics
        int originalTokens = session.getOriginalText().length() / 4;
        int resultTokens = results.stream()
                .mapToInt(r -> r.getSnippet().length() / 4)
                .sum();

        return QueryResponseV2.builder()
                .query(request.getQuery())
                .results(results)
                .resultCount(results.size())
                .metrics(TokenMetrics.of(originalTokens, resultTokens, results.size(), document.getChunkCount()))
                .build();
    }

    /**
     * Get session info with disclosure level control.
     */
    public AnalyzeResponseV2 getSessionInfo(String sessionId, DisclosureLevel disclosureLevel) {
        AnalysisSession session = chunkStore.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found or expired: " + sessionId);
        }

        String documentId = session.getDocumentId();
        Document document = documentId != null ? documentStore.get(documentId) : null;

        if (document == null) {
            // Rebuild from V1 chunks
            document = loadDocumentFromSession(session);
        }

        if (document == null) {
            throw new IllegalArgumentException("Document not found");
        }

        List<ChunkViewV2> chunkViews = buildChunkViews(
                document.getRootChunks(),
                disclosureLevel,
                document
        );

        int originalTokens = session.getOriginalText().length() / 4;
        int processedTokens = estimateTokensForDisclosure(chunkViews, disclosureLevel);

        return AnalyzeResponseV2.builder()
                .sessionId(sessionId)
                .documentId(document.getId())
                .documentTitle(document.getTitle())
                .disclosureLevel(disclosureLevel)
                .chunks(chunkViews)
                .totalChunkCount(document.getChunkCount())
                .metrics(TokenMetrics.of(originalTokens, processedTokens, 0, document.getChunkCount()))
                .build();
    }

    /**
     * Delete session.
     */
    public void deleteSession(String sessionId) {
        AnalysisSession session = chunkStore.getSession(sessionId);
        if (session != null) {
            String documentId = session.getDocumentId();
            if (documentId != null) {
                documentStore.remove(documentId);
            }
        }
        chunkStore.deleteSession(sessionId);
    }

    // ========== Helper Methods ==========

    private HierarchicalChunkStrategy selectStrategy(String strategyName) {
        return switch (strategyName != null ? strategyName.toLowerCase() : "semantic") {
            case "outline" -> outlineChunker;
            case "fixed" -> fixedChunker;
            default -> semanticChunker;
        };
    }

    private List<ChunkViewV2> buildChunkViews(
            List<HierarchicalChunk> chunks,
            DisclosureLevel level,
            Document document
    ) {
        return chunks.stream()
                .map(c -> toChunkViewV2(c, document, level))
                .toList();
    }

    private ChunkViewV2 toChunkViewV2(
            HierarchicalChunk chunk,
            Document document,
            DisclosureLevel level
    ) {
        return switch (level) {
            case OUTLINE -> ChunkViewV2.toOutlineView(toFullChunkViewV2(chunk));
            case SUMMARY -> ChunkViewV2.toSummaryView(toFullChunkViewV2(chunk));
            case EXPANDED -> toExpandedChunkViewV2(chunk);
            case FULL -> toFullChunkViewV2(chunk);
        };
    }

    private ChunkViewV2 toFullChunkViewV2(HierarchicalChunk chunk) {
        return ChunkViewV2.builder()
                .id(chunk.getId())
                .title(chunk.getTitle())
                .level(chunk.getLevel())
                .parentId(chunk.getParentId())
                .childIds(chunk.getChildIds())
                .summary(chunk.getSummary())
                .content(chunk.getContent())
                .charCount(chunk.getCharCount())
                .estimatedTokens(chunk.getCharCount() / 4)
                .build();
    }

    private ChunkViewV2 toExpandedChunkViewV2(HierarchicalChunk chunk) {
        return ChunkViewV2.builder()
                .id(chunk.getId())
                .title(chunk.getTitle())
                .level(chunk.getLevel())
                .parentId(chunk.getParentId())
                .childIds(chunk.getChildIds())
                .summary(chunk.getSummary())
                .charCount(chunk.getCharCount())
                .estimatedTokens(chunk.getCharCount() / 4)
                .build();
    }

    private ChunkViewV2 toChunkViewV2Full(HierarchicalChunk chunk) {
        return ChunkViewV2.builder()
                .id(chunk.getId())
                .title(chunk.getTitle())
                .level(chunk.getLevel())
                .parentId(chunk.getParentId())
                .childIds(chunk.getChildIds())
                .content(chunk.getContent())
                .summary(chunk.getSummary())
                .charCount(chunk.getCharCount())
                .estimatedTokens(chunk.getCharCount() / 4)
                .build();
    }

    private void loadChunkContent(HierarchicalChunk chunk, AnalysisSession session) {
        if (chunk.hasContent()) {
            return;
        }

        // Try to load from session V1 chunks
        com.zff.dismantle.chunk.Chunk v1Chunk = session.getV1Chunk(chunk.getId());
        if (v1Chunk != null) {
            chunk.setContent(v1Chunk.getContent());
            return;
        }

        // Try to extract from original text using offsets
        if (chunk.getStartOffset() >= 0 && chunk.getEndOffset() <= session.getOriginalText().length()) {
            String content = session.getOriginalText().substring(
                    chunk.getStartOffset(),
                    chunk.getEndOffset()
            );
            chunk.setContent(content);
        }
    }

    private void storeDocumentInSession(AnalysisSession session, Document document) {
        // Store V1 chunks for backward compatibility
        for (HierarchicalChunk hChunk : document.getLeafChunks()) {
            if (hChunk.hasContent()) {
                com.zff.dismantle.chunk.Chunk v1Chunk = com.zff.dismantle.chunk.Chunk.builder()
                        .id(hChunk.getId())
                        .content(hChunk.getContent())
                        .title(hChunk.getTitle())
                        .summary(hChunk.getSummary())
                        .index(hChunk.getIndex())
                        .startOffset(hChunk.getStartOffset())
                        .endOffset(hChunk.getEndOffset())
                        .build();
                session.addChunk(v1Chunk);
            }
        }
    }

    private Document loadDocumentFromSession(AnalysisSession session) {
        if (!session.hasV1Chunks()) {
            return null;
        }

        Document document = Document.of(session.getOriginalText());

        for (com.zff.dismantle.chunk.Chunk v1Chunk : session.getV1Chunks().values()) {
            HierarchicalChunk hChunk = HierarchicalChunk.builder()
                    .id(v1Chunk.getId())
                    .content(v1Chunk.getContent())
                    .title(v1Chunk.getTitle())
                    .summary(v1Chunk.getSummary())
                    .level(ChunkLevel.PARAGRAPH)
                    .index(v1Chunk.getIndex())
                    .startOffset(v1Chunk.getStartOffset())
                    .endOffset(v1Chunk.getEndOffset())
                    .charCount(v1Chunk.getContent() != null ? v1Chunk.getContent().length() : 0)
                    .build();
            document.addChunk(hChunk);
        }

        return document;
    }

    private RetrieveResponseV2 retrieveV1(RetrieveRequestV2 request, AnalysisSession session) {
        // Fallback to V1 behavior
        List<com.zff.dismantle.chunk.Chunk> selectedChunks = session.getChunks(request.getChunkIds());
        if (selectedChunks.isEmpty()) {
            throw new IllegalArgumentException("No valid chunks found for given IDs");
        }

        StringBuilder mergedText = new StringBuilder();
        for (com.zff.dismantle.chunk.Chunk chunk : selectedChunks) {
            mergedText.append("[").append(chunk.getTitle()).append("]\n");
            mergedText.append(chunk.getContent()).append("\n\n");
        }

        String mergedTextStr = mergedText.toString().trim();

        List<ChunkViewV2> selectedChunkViews = selectedChunks.stream()
                .map(c -> ChunkViewV2.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .content(c.getContent())
                        .charCount(c.getContent() != null ? c.getContent().length() : 0)
                        .build())
                .toList();

        int originalTokens = session.getOriginalText().length() / 4;
        int mergedTokens = mergedTextStr.length() / 4;

        return RetrieveResponseV2.builder()
                .mergedText(mergedTextStr)
                .selectedChunks(selectedChunkViews)
                .disclosureLevel(DisclosureLevel.FULL)
                .metrics(TokenMetrics.of(originalTokens, mergedTokens, selectedChunks.size(), session.getChunkCount()))
                .build();
    }

    private QueryResponseV2 queryV1(QueryRequestV2 request, AnalysisSession session) {
        List<QueryResponseV2.QueryResultView> results = new ArrayList<>();
        String query = request.getQuery().toLowerCase();

        for (com.zff.dismantle.chunk.Chunk chunk : session.getV1Chunks().values()) {
            String content = chunk.getContent().toLowerCase();
            String title = chunk.getTitle().toLowerCase();

            double score = calculateKeywordScore(query, title, content);

            if (score >= request.getMinScore()) {
                String snippet = extractSnippet(chunk.getContent(), query);
                String highlightedSnippet = request.isHighlight()
                        ? highlightMatches(snippet, query)
                        : snippet;

                results.add(QueryResponseV2.QueryResultView.builder()
                        .chunkId(chunk.getId())
                        .title(chunk.getTitle())
                        .level("PARAGRAPH")
                        .score(score)
                        .snippet(snippet)
                        .highlightedSnippet(highlightedSnippet)
                        .charCount(chunk.getContent() != null ? chunk.getContent().length() : 0)
                        .build());
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (results.size() > request.getMaxResults()) {
            results = results.subList(0, request.getMaxResults());
        }

        int originalTokens = session.getOriginalText().length() / 4;
        int resultTokens = results.stream()
                .mapToInt(r -> r.getSnippet().length() / 4)
                .sum();

        return QueryResponseV2.builder()
                .query(request.getQuery())
                .results(results)
                .resultCount(results.size())
                .metrics(TokenMetrics.of(originalTokens, resultTokens, results.size(), session.getV1Chunks().size()))
                .build();
    }

    private double calculateKeywordScore(String query, String title, String content) {
        double score = 0.0;

        // Title match (high weight)
        if (title.contains(query)) {
            score += 0.5;
        }

        // Content match
        int occurrences = countOccurrences(content, query);
        score += Math.min(0.5, occurrences * 0.05);

        return Math.min(1.0, score);
    }

    private int countOccurrences(String text, String query) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(query, index)) != -1) {
            count++;
            index += query.length();
        }
        return count;
    }

    private String extractSnippet(String content, String query) {
        int pos = content.toLowerCase().indexOf(query.toLowerCase());
        if (pos < 0) {
            return content.substring(0, Math.min(100, content.length()));
        }

        int start = Math.max(0, pos - 30);
        int end = Math.min(content.length(), pos + query.length() + 70);

        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";

        return snippet;
    }

    private String highlightMatches(String text, String query) {
        String regex = Pattern.quote(query);
        return text.replaceAll("(?i)" + regex, "<mark>$0</mark>");
    }

    private int estimateTokensForDisclosure(List<ChunkViewV2> chunks, DisclosureLevel level) {
        return switch (level) {
            case OUTLINE -> chunks.size() * 10; // ~10 tokens per chunk (id + title)
            case SUMMARY -> chunks.size() * 25; // ~25 tokens per chunk (+ summary)
            case EXPANDED -> chunks.size() * 50; // ~50 tokens per chunk (+ metadata)
            case FULL -> chunks.stream().mapToInt(c -> c.getEstimatedTokens()).sum();
        };
    }
}
