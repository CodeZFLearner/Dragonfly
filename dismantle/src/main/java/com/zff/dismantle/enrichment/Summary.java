package com.zff.dismantle.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a generated summary with metadata.
 *
 * @see SummaryGenerator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summary {

    /**
     * The summary text.
     */
    private String text;

    /**
     * The level of detail (BRIEF or DETAILED).
     */
    private SummaryLevel level;

    /**
     * Estimated token count of the summary.
     */
    private int tokenCount;

    /**
     * Creates a brief summary (1 sentence).
     *
     * @param text the summary text
     * @return new Summary instance
     */
    public static Summary brief(String text) {
        return Summary.builder()
                .text(text)
                .level(SummaryLevel.BRIEF)
                .tokenCount(estimateTokens(text))
                .build();
    }

    /**
     * Creates a detailed summary (multiple sentences).
     *
     * @param text the summary text
     * @return new Summary instance
     */
    public static Summary detailed(String text) {
        return Summary.builder()
                .text(text)
                .level(SummaryLevel.DETAILED)
                .tokenCount(estimateTokens(text))
                .build();
    }

    /**
     * Estimates token count from character count.
     */
    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4; // Rough estimate for mixed Chinese/English
    }

    /**
     * Checks if this summary is empty.
     *
     * @return true if text is null or blank
     */
    public boolean isEmpty() {
        return text == null || text.isBlank();
    }

    /**
     * Gets a truncated version of the summary.
     *
     * @param maxLength maximum character length
     * @return truncated summary text
     */
    public String truncate(int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
