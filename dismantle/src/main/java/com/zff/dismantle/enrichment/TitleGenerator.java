package com.zff.dismantle.enrichment;

import com.zff.dismantle.core.HierarchicalChunk;

/**
 * Strategy interface for generating titles from text content.
 *
 * <p>Implementations can use different approaches:
 * <ul>
 *   <li>{@link RuleBasedTitleGenerator} - Extract first line or heading</li>
 *   <li>{@link LlmTitleGenerator} - Use LLM to generate concise titles</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * TitleGenerator generator = new LlmTitleGenerator(ollamaClient);
 * String title = generator.generate(content, TitleContext.builder()
 *     .maxLength(50)
 *     .language("en")
 *     .build());
 * }</pre>
 *
 * @see RuleBasedTitleGenerator
 * @see LlmTitleGenerator
 */
public interface TitleGenerator {

    /**
     * Returns the name of this generator.
     *
     * @return generator name
     */
    String getName();

    /**
     * Generates a title from the given content.
     *
     * @param content the text content to generate a title for
     * @return generated title
     */
    default String generate(String content) {
        return generate(content, TitleContext.defaults());
    }

    /**
     * Generates a title from the given content with configuration.
     *
     * @param content the text content
     * @param context configuration for title generation
     * @return generated title
     */
    String generate(String content, TitleContext context);

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
