package com.zff.dismantle.retrieval;

import com.zff.dismantle.core.HierarchicalChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword-based retriever using BM25-style scoring.
 *
 * <p>This retriever calculates relevance scores based on:
 * <ul>
 *   <li>Title match (high weight: 0.5)</li>
 *   <li>Content keyword frequency</li>
 *   <li>Keyword density</li>
 * </ul>
 *
 * <h2>Scoring Algorithm</h2>
 * <pre>
 * score = titleMatchScore + contentScore
 * titleMatchScore = 0.5 if title contains query
 * contentScore = min(0.5, occurrences * 0.05)
 * </pre>
 */
@Component
public class KeywordRetriever implements Retriever {

    @Override
    public String getName() {
        return "keyword";
    }

    @Override
    public List<RetrievalResult> retrieve(
            String query,
            List<HierarchicalChunk> chunks,
            int maxResults
    ) {
        if (query == null || query.isBlank() || chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        String normalizedQuery = query.toLowerCase().trim();
        List<RetrievalResult> results = new ArrayList<>();

        for (HierarchicalChunk chunk : chunks) {
            RetrievalResult result = scoreChunk(normalizedQuery, chunk);
            if (result.getScore() > 0) {
                results.add(result);
            }
        }

        // Sort by score descending
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Limit results
        if (results.size() > maxResults) {
            return results.subList(0, maxResults);
        }

        return results;
    }

    /**
     * Scores a single chunk against the query.
     */
    private RetrievalResult scoreChunk(String query, HierarchicalChunk chunk) {
        double score = 0.0;

        // Title match (high weight)
        String title = chunk.getTitle() != null ? chunk.getTitle().toLowerCase() : "";
        boolean titleMatch = title.contains(query);
        if (titleMatch) {
            score += 0.5;
        }

        // Content match
        String content = chunk.getContent() != null ? chunk.getContent().toLowerCase() : "";
        if (!content.isEmpty()) {
            int occurrences = countOccurrences(content, query);
            double contentScore = Math.min(0.5, occurrences * 0.05);
            score += contentScore;
        }

        // Extract snippet
        String snippet = extractSnippet(content, query);

        return RetrievalResult.builder()
                .chunk(chunk)
                .score(Math.min(1.0, score))
                .snippet(snippet)
                .build();
    }

    /**
     * Counts occurrences of query in text.
     */
    private int countOccurrences(String text, String query) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(query, index)) != -1) {
            count++;
            index += query.length();
        }
        return count;
    }

    /**
     * Extracts a snippet containing the query match.
     */
    private String extractSnippet(String content, String query) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        int pos = content.indexOf(query);
        if (pos < 0) {
            // Try case-insensitive
            pos = content.toLowerCase().indexOf(query.toLowerCase());
        }

        if (pos < 0) {
            return content.substring(0, Math.min(100, content.length()));
        }

        int start = Math.max(0, pos - 30);
        int end = Math.min(content.length(), pos + query.length() + 70);

        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";

        return snippet.trim();
    }
}
