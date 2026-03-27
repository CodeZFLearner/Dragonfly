package com.zff.dismantle.enrichment;

import com.zff.dismantle.ollama.SimpleOllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM-based title generator using Ollama or compatible APIs.
 *
 * <p>This generator uses a local LLM to generate concise, semantically
 * meaningful titles from text content.
 *
 * <h2>Advantages</h2>
 * <ul>
 *   <li>Generates semantically meaningful titles</li>
 *   <li>Handles various document formats well</li>
 *   <li>Can be configured for different styles</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Requires running LLM server (e.g., Ollama)</li>
 *   <li>Slower than rule-based approaches</li>
 *   <li>May produce inconsistent results</li>
 * </ul>
 *
 * <h2>Fallback Behavior</h2>
 * If the LLM call fails or times out, falls back to {@link RuleBasedTitleGenerator}.
 */
@Slf4j
@Component
public class LlmTitleGenerator implements TitleGenerator {

    private static final String DEFAULT_PROMPT =
            "Generate a concise title (max %d characters) for the following text. " +
            "Return ONLY the title, no quotes or explanations.\n\n" +
            "Text: %s";

    private final SimpleOllamaClient ollamaClient;
    private final RuleBasedTitleGenerator fallbackGenerator;

    public LlmTitleGenerator() {
        this.ollamaClient = new SimpleOllamaClient("http://localhost:11434");
        this.fallbackGenerator = new RuleBasedTitleGenerator();
    }

    public LlmTitleGenerator(SimpleOllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.fallbackGenerator = new RuleBasedTitleGenerator();
    }

    @Override
    public String getName() {
        return "llm";
    }

    @Override
    public String generate(String content) {
        return generate(content, TitleContext.defaults());
    }

    @Override
    public String generate(String content, TitleContext context) {
        if (content == null || content.isBlank()) {
            return "Untitled";
        }

        try {
            // Prepare content snippet (limit context for LLM)
            String contentSnippet = prepareContentForLlm(content, context);

            // Build prompt
            String prompt = String.format(
                    DEFAULT_PROMPT,
                    context.getMaxLength(),
                    contentSnippet
            );

            // Call LLM
            String title = ollamaClient.nameTitle(prompt);

            if (title != null && !title.isBlank()) {
                // Clean up the response
                title = title.trim()
                        .replaceAll("^\"|\"$", "") // Remove surrounding quotes
                        .replaceAll("^'+'$", ""); // Remove surrounding single quotes

                if (!title.isBlank()) {
                    return title;
                }
            }

            // Fallback to rule-based
            log.debug("LLM returned empty title, using fallback");
            return fallbackGenerator.generate(content, context);

        } catch (Exception e) {
            log.warn("LLM title generation failed, using fallback: {}", e.getMessage());
            return fallbackGenerator.generate(content, context);
        }
    }

    /**
     * Prepares content snippet for LLM processing.
     * Limits length while preserving beginning context.
     */
    private String prepareContentForLlm(String content, TitleContext context) {
        // LLMs work better with limited context for title generation
        // Usually the first 500-1000 characters contain enough information
        int maxLength = Math.min(content.length(), 1000);

        String snippet = content.substring(0, maxLength);

        // Try to end at a natural boundary
        if (maxLength < content.length()) {
            int lastSentenceEnd = Math.max(
                    snippet.lastIndexOf('.'),
                    Math.max(snippet.lastIndexOf('!'), snippet.lastIndexOf('?'))
            );

            if (lastSentenceEnd > maxLength / 2) {
                snippet = snippet.substring(0, lastSentenceEnd + 1);
            }
        }

        return snippet.trim();
    }
}
