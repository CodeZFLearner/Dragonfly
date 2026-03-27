package com.zff.dismantle.enrichment;

/**
 * Level of detail for summaries.
 *
 * @see Summary
 * @see SummaryGenerator
 */
public enum SummaryLevel {

    /**
     * Brief summary: 1 sentence, ~20-50 words.
     * Best for quick scanning and comparison.
     */
    BRIEF,

    /**
     * Detailed summary: 2-4 sentences, ~100-200 words.
     * Best for understanding main points without reading full content.
     */
    DETAILED
}
