package com.zff.dismantle.retrieval;

import com.zff.dismantle.core.HierarchicalChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid retriever that combines keyword and semantic scores.
 *
 * <p>This retriever uses a weighted combination of:
 * <ul>
 *   <li>Keyword-based scoring (BM25 style)</li>
 *   <li>Semantic similarity (future, when embeddings are available)</li>
 * </ul>
 *
 * <h2>Scoring Formula</h2>
 * <pre>
 * hybridScore = alpha * keywordScore + (1-alpha) * semanticScore
 * </pre>
 *
 * Currently falls back to {@link KeywordRetriever} since semantic
 * embeddings are not yet implemented.
 */
@Component
public class HybridRetriever implements Retriever {

    private final KeywordRetriever keywordRetriever;
    private final double alpha; // Weight for keyword score

    public HybridRetriever() {
        this(new KeywordRetriever(), 0.7); // Default: 70% keyword, 30% semantic
    }

    public HybridRetriever(KeywordRetriever keywordRetriever, double alpha) {
        this.keywordRetriever = keywordRetriever;
        this.alpha = Math.max(0.0, Math.min(1.0, alpha));
    }

    @Override
    public String getName() {
        return "hybrid";
    }

    @Override
    public List<RetrievalResult> retrieve(
            String query,
            List<HierarchicalChunk> chunks,
            int maxResults
    ) {
        // For now, delegate to keyword retriever
        // Semantic retrieval will be added when embeddings are supported
        return keywordRetriever.retrieve(query, chunks, maxResults);
    }

    @Override
    public List<RetrievalResult> retrieve(
            String query,
            List<HierarchicalChunk> chunks,
            int maxResults,
            double minScore
    ) {
        // For now, delegate to keyword retriever
        return keywordRetriever.retrieve(query, chunks, maxResults, minScore);
    }

    /**
     * Sets the alpha weight for keyword scoring.
     *
     * @param alpha weight for keyword score (0.0 - 1.0)
     * @return this instance for chaining
     */
    public HybridRetriever withAlpha(double alpha) {
        return new HybridRetriever(this.keywordRetriever, alpha);
    }
}
