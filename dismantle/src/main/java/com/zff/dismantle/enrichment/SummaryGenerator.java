package com.zff.dismantle.enrichment;

/**
 * Strategy interface for generating summaries from text content.
 *
 * <p>Implementations can use different approaches:
 * <ul>
 *   <li>{@link RuleBasedSummaryGenerator} - Extract key sentences</li>
 *   <li>{@link LlmSummaryGenerator} - Use LLM to generate summaries</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * SummaryGenerator generator = new LlmSummaryGenerator(ollamaClient);
 * Summary summary = generator.generate(content, SummaryLevel.BRIEF);
 * }</pre>
 *
 * @see Summary
 * @see SummaryLevel
 */
public interface SummaryGenerator {

    /**
     * Returns the name of this generator.
     *
     * @return generator name
     */
    String getName();

    /**
     * Generates a summary from the given content.
     *
     * @param content the text content to summarize
     * @param level the desired summary detail level
     * @return generated summary
     */
    Summary generate(String content, SummaryLevel level);

    /**
     * Generates a brief summary from the given content.
     *
     * @param content the text content to summarize
     * @return generated summary
     */
    default Summary generateBrief(String content) {
        return generate(content, SummaryLevel.BRIEF);
    }

    /**
     * Generates a detailed summary from the given content.
     *
     * @param content the text content to summarize
     * @return generated summary
     */
    default Summary generateDetailed(String content) {
        return generate(content, SummaryLevel.DETAILED);
    }

    /**
     * Checks if this generator supports the given content.
     *
     * @param content the content to check
     * @return true if supported
     */
    default boolean supports(String content) {
        return content != null && !content.isBlank();
    }
}
