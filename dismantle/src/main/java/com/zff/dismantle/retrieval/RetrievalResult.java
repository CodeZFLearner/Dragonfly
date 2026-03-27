package com.zff.dismantle.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a retrieval result with relevance score.
 *
 * @see Retriever
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /**
     * The matched chunk.
     */
    private HierarchicalChunk chunk;

    /**
     * Relevance score (0.0 - 1.0).
     */
    private double score;

    /**
     * Matched snippet from the content.
     */
    private String snippet;

    /**
     * Highlighted snippet (if highlighting was requested).
     */
    private String highlightedSnippet;

    /**
     * Creates a new result with a highlighted snippet.
     *
     * @param query the query to highlight
     * @param prefix highlight prefix (default: <mark>)
     * @param suffix highlight suffix (default: </mark>)
     * @return new result with highlighted snippet
     */
    public RetrievalResult withHighlight(
            String query,
            String prefix,
            String suffix
    ) {
        if (this.snippet == null || this.snippet.isEmpty()) {
            return this;
        }

        String regex = java.util.regex.Pattern.quote(query);
        String replacement = prefix + "$0" + suffix;
        String highlighted = this.snippet.replaceAll("(?i)" + regex, replacement);

        return RetrievalResult.builder()
                .chunk(this.chunk)
                .score(this.score)
                .snippet(this.snippet)
                .highlightedSnippet(highlighted)
                .build();
    }
}
