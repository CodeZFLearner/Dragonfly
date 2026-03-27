package com.zff.dismantle.retrieval;

import com.zff.dismantle.core.HierarchicalChunk;

import java.util.List;

/**
 * Strategy interface for retrieving relevant chunks based on a query.
 *
 * <p>Implementations can use different approaches:
 * <ul>
 *   <li>{@link KeywordRetriever} - BM25/TF-IDF style keyword matching</li>
 *   <li>{@link SemanticRetriever} - Embedding-based semantic search (future)</li>
 *   <li>{@link HybridRetriever} - Combine keyword and semantic scoring</li>
 * </ul>
 *
 * @see KeywordRetriever
 */
public interface Retriever {

    /**
     * Returns the name of this retriever.
     *
     * @return retriever name
     */
    String getName();

    /**
     * Retrieves relevant chunks for the given query.
     *
     * @param query the search query
     * @param chunks all available chunks to search from
     * @param maxResults maximum number of results to return
     * @return list of retrieval results with scores
     */
    List<RetrievalResult> retrieve(String query, List<HierarchicalChunk> chunks, int maxResults);

    /**
     * Retrieves relevant chunks for the given query with a minimum score threshold.
     *
     * @param query the search query
     * @param chunks all available chunks to search from
     * @param maxResults maximum number of results to return
     * @param minScore minimum score threshold (0.0 - 1.0)
     * @return list of retrieval results with scores
     */
    default List<RetrievalResult> retrieve(
            String query,
            List<HierarchicalChunk> chunks,
            int maxResults,
            double minScore
    ) {
        return retrieve(query, chunks, maxResults).stream()
                .filter(r -> r.getScore() >= minScore)
                .toList();
    }
}
