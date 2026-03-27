package com.zff.dismantle.enrichment;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rule-based summary generator that extracts key sentences.
 *
 * <p>This generator uses heuristic approaches to create summaries:
 * <ol>
 *   <li>For BRIEF: Extract the first meaningful sentence</li>
 *   <li>For DETAILED: Extract first sentence + key middle sentences + last sentence</li>
 * </ol>
 *
 * <h2>Advantages</h2>
 * <ul>
 *   <li>Fast - no LLM calls required</li>
 *   <li>Deterministic - same input always produces same output</li>
 *   <li>Works offline</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>May not capture semantic meaning well</li>
 *   <li>Depends on document structure (topic sentences at beginning)</li>
 * </ul>
 */
@Component
public class RuleBasedSummaryGenerator implements SummaryGenerator {

    private static final int BRIEF_MAX_SENTENCES = 1;
    private static final int DETAILED_MAX_SENTENCES = 4;

    @Override
    public String getName() {
        return "rule-based";
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

        List<String> sentences = extractSentences(content);

        if (sentences.isEmpty()) {
            return Summary.builder()
                    .text("")
                    .level(level)
                    .tokenCount(0)
                    .build();
        }

        String summaryText = switch (level) {
            case BRIEF -> generateBriefSummary(sentences);
            case DETAILED -> generateDetailedSummary(sentences);
        };

        return Summary.builder()
                .text(summaryText)
                .level(level)
                .tokenCount(summaryText.length() / 4)
                .build();
    }

    /**
     * Extracts sentences from content.
     */
    private List<String> extractSentences(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        // Split by sentence-ending punctuation
        String[] parts = content.split("(?<=[.!?。！？])\\s*");

        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> s.length() > 10) // Filter out very short fragments
                .toList();
    }

    /**
     * Generates a brief summary (first sentence).
     */
    private String generateBriefSummary(List<String> sentences) {
        if (sentences.isEmpty()) {
            return "";
        }

        // Return first meaningful sentence
        return sentences.get(0);
    }

    /**
     * Generates a detailed summary (first + middle + last sentences).
     */
    private String generateDetailedSummary(List<String> sentences) {
        if (sentences.isEmpty()) {
            return "";
        }

        if (sentences.size() <= DETAILED_MAX_SENTENCES) {
            // Not enough sentences, return all
            return String.join(" ", sentences);
        }

        // Select key sentences
        StringBuilder summary = new StringBuilder();

        // First sentence (introduces topic)
        summary.append(sentences.get(0));

        // Middle sentences (sample evenly)
        int middleCount = DETAILED_MAX_SENTENCES - 2;
        int step = sentences.size() / (middleCount + 1);

        for (int i = 1; i < middleCount + 1; i++) {
            int index = Math.min(i * step, sentences.size() - 2);
            summary.append(" ").append(sentences.get(index));
        }

        // Last sentence (conclusion if present)
        String lastSentence = sentences.get(sentences.size() - 1);
        if (!lastSentence.equals(summary.substring(summary.length() - Math.min(50, summary.length())))) {
            summary.append(" ").append(lastSentence);
        }

        return summary.toString();
    }
}
