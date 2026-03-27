package com.zff.dismantle.enrichment;

import com.zff.dismantle.ollama.SimpleOllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM-based summary generator using Ollama or compatible APIs.
 *
 * <p>This generator uses a local LLM to generate concise, semantically
 * meaningful summaries from text content.
 *
 * <h2>Advantages</h2>
 * <ul>
 *   <li>Generates coherent, readable summaries</li>
 *   <li>Captures semantic meaning better than extraction</li>
 *   <li>Can be configured for different styles and lengths</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Requires running LLM server (e.g., Ollama)</li>
 *   <li>Slower than rule-based approaches</li>
 *   <li>May hallucinate or miss key details</li>
 * </ul>
 *
 * <h2>Fallback Behavior</h2>
 * If the LLM call fails or times out, falls back to {@link RuleBasedSummaryGenerator}.
 */
@Slf4j
@Component
public class LlmSummaryGenerator implements SummaryGenerator {

    private static final String BRIEF_PROMPT =
            "Summarize the following text in ONE concise sentence. " +
            "Return ONLY the summary, no quotes or explanations.\n\n" +
            "Text: %s";

    private static final String DETAILED_PROMPT =
            "Summarize the following text in 3-4 sentences. " +
            "Capture the main points and key information. " +
            "Return ONLY the summary, no quotes or explanations.\n\n" +
            "Text: %s";

    private final SimpleOllamaClient ollamaClient;
    private final RuleBasedSummaryGenerator fallbackGenerator;

    public LlmSummaryGenerator() {
        this.ollamaClient = new SimpleOllamaClient("http://localhost:11434");
        this.fallbackGenerator = new RuleBasedSummaryGenerator();
    }

    public LlmSummaryGenerator(SimpleOllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.fallbackGenerator = new RuleBasedSummaryGenerator();
    }

    @Override
    public String getName() {
        return "llm";
    }

    @Override
    public Summary generate(String content, SummaryLevel level) {
        if (content == null || content.isBlank()) {
            return Summary.builder()
                    .text("")
                    .level(level)
                    .tokenCount(0)
                    .build();
        }

        try {
            // Prepare content snippet
            String contentSnippet = prepareContentForLlm(content);

            // Build prompt based on level
            String prompt = switch (level) {
                case BRIEF -> String.format(BRIEF_PROMPT, contentSnippet);
                case DETAILED -> String.format(DETAILED_PROMPT, contentSnippet);
            };

            // Call LLM
            String summaryText = ollamaClient.nameTitle(prompt);

            if (summaryText != null && !summaryText.isBlank()) {
                // Clean up the response
                summaryText = summaryText.trim()
                        .replaceAll("^\"|\"$", "")
                        .replaceAll("^'+'$", "");

                if (!summaryText.isBlank()) {
                    return Summary.builder()
                            .text(summaryText)
                            .level(level)
                            .tokenCount(summaryText.length() / 4)
                            .build();
                }
            }

            // Fallback to rule-based
            log.debug("LLM returned empty summary, using fallback");
            return fallbackGenerator.generate(content, level);

        } catch (Exception e) {
            log.warn("LLM summary generation failed, using fallback: {}", e.getMessage());
            return fallbackGenerator.generate(content, level);
        }
    }

    /**
     * Prepares content snippet for LLM processing.
     */
    private String prepareContentForLlm(String content) {
        // Limit to ~2000 characters for LLM context
        int maxLength = Math.min(content.length(), 2000);

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
