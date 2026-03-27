package com.zff.dismantle.enrichment;

import org.springframework.stereotype.Component;

/**
 * Rule-based title generator that extracts titles using heuristics.
 *
 * <p>This generator uses the following strategies in order:
 * <ol>
 *   <li>Extract Markdown heading (remove # symbols)</li>
 *   <li>Extract first line if it looks like a title</li>
 *   <li>Extract first sentence (up to first period)</li>
 *   <li>Truncate content to max length</li>
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
 *   <li>May not produce semantically meaningful titles</li>
 *   <li>Depends on document structure quality</li>
 * </ul>
 */
@Component
public class RuleBasedTitleGenerator implements TitleGenerator {

    @Override
    public String getName() {
        return "rule-based";
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

        String trimmed = content.trim();

        // Strategy 1: Extract Markdown heading
        String headingTitle = extractMarkdownHeading(trimmed);
        if (headingTitle != null && !headingTitle.isBlank()) {
            return finalizeTitle(headingTitle, context);
        }

        // Strategy 2: Extract first line if it's short and title-like
        String lineTitle = extractFirstLine(trimmed);
        if (lineTitle != null && isTitleLike(lineTitle, context)) {
            return finalizeTitle(lineTitle, context);
        }

        // Strategy 3: Extract first sentence
        String sentenceTitle = extractFirstSentence(trimmed);
        if (sentenceTitle != null && sentenceTitle.length() >= context.getMinLength()) {
            return finalizeTitle(sentenceTitle, context);
        }

        // Fallback: Truncate content
        return finalizeTitle(trimmed, context);
    }

    /**
     * Extracts Markdown heading from content.
     */
    private String extractMarkdownHeading(String content) {
        String[] lines = content.split("\\n");
        if (lines.length == 0) {
            return null;
        }

        String firstLine = lines[0].trim();
        if (firstLine.startsWith("#")) {
            // Remove # symbols and leading whitespace
            return firstLine.replaceAll("^#+\\s*", "");
        }

        return null;
    }

    /**
     * Extracts the first line of content.
     */
    private String extractFirstLine(String content) {
        String[] lines = content.split("\\n");
        if (lines.length == 0) {
            return null;
        }
        return lines[0].trim();
    }

    /**
     * Extracts the first sentence from content.
     */
    private String extractFirstSentence(String content) {
        // Look for sentence-ending punctuation
        int endPos = -1;
        for (int i = 0; i < Math.min(content.length(), 200); i++) {
            char c = content.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                endPos = i;
                break;
            }
        }

        if (endPos > 0) {
            return content.substring(0, endPos).trim();
        }

        return content.length() > 100 ? content.substring(0, 100) : content;
    }

    /**
     * Checks if a string looks like a title.
     */
    private boolean isTitleLike(String text, TitleContext context) {
        if (text == null || text.isBlank()) {
            return false;
        }

        // Too long doesn't look like a title
        if (text.length() > context.getMaxLength() * 1.5) {
            return false;
        }

        // Contains too many periods doesn't look like a title
        long periodCount = text.chars().filter(c -> c == '.').count();
        if (periodCount > 2) {
            return false;
        }

        // Single word might not be descriptive enough (unless very short)
        if (!text.contains(" ") && text.length() < 4) {
            return false;
        }

        return true;
    }

    /**
     * Finalizes a title by applying length constraints and formatting.
     */
    private String finalizeTitle(String title, TitleContext context) {
        if (title == null || title.isBlank()) {
            return "Untitled";
        }

        String result = title.trim();

        // Apply length constraints
        if (result.length() > context.getMaxLength()) {
            if (context.isTruncateWithEllipsis()) {
                // Find word boundary
                int breakPoint = result.lastIndexOf(' ', context.getMaxLength() - 3);
                if (breakPoint > context.getMaxLength() / 2) {
                    result = result.substring(0, breakPoint) + "...";
                } else {
                    result = result.substring(0, context.getMaxLength() - 3) + "...";
                }
            } else {
                result = result.substring(0, context.getMaxLength());
            }
        }

        // Ensure minimum length (pad if needed - edge case)
        if (result.length() < context.getMinLength() && result.length() < title.length()) {
            // Try to extend
            int extendedLength = Math.min(context.getMinLength(), title.length());
            result = title.substring(0, extendedLength).trim();
        }

        // Casing
        if (!context.isPreserveCasing() && !isChinese(title)) {
            result = toTitleCase(result);
        }

        return result;
    }

    /**
     * Converts a string to title case.
     */
    private String toTitleCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean uppercaseNext = true;

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                uppercaseNext = true;
            } else if (uppercaseNext) {
                result.append(Character.toUpperCase(c));
                uppercaseNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Checks if text contains primarily Chinese characters.
     */
    private boolean isChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        int chineseCount = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\\u4e00' && c <= '\\u9fa5') {
                chineseCount++;
            }
        }

        return chineseCount > text.length() / 3;
    }
}
