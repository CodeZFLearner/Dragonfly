package com.zff.dismantle.core;

/**
 * Represents the level of detail disclosed in a progressive disclosure response.
 *
 * <p>Progressive Disclosure is an interaction pattern that shows information
 * incrementally, revealing more detail as requested. This is crucial for
 * token-efficient LLM agent interactions.
 *
 * <h2>Disclosure Levels</h2>
 *
 * <table>
 *   <tr><th>Level</th><th>Name</th><th>Content</th><th>Token Usage</th></tr>
 *   <tr><td>{@code OUTLINE}</td><td>Outline</td><td>ID + Title + Level</td><td>~5%</td></tr>
 *   <tr><td>{@code SUMMARY}</td><td>Summary</td><td>Outline + Summary + Keywords</td><td>~15%</td></tr>
 *   <tr><td>{@code EXPANDED}</td><td>Expanded</td><td>Summary + Metadata + Children</td><td>~30%</td></tr>
 *   <tr><td>{@code FULL}</td><td>Full Content</td><td>Complete text</td><td>100%</td></tr>
 * </table>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Step 1: Get outline (minimal tokens)
 * Document outline = document.toDisclosureView(DisclosureLevel.OUTLINE);
 *
 * // Step 2: Agent selects relevant sections based on titles
 *
 * // Step 3: Expand selected sections (moderate tokens)
 * Document summary = document.toDisclosureView(DisclosureLevel.SUMMARY);
 *
 * // Step 4: Retrieve full content of chosen chunks
 * Document full = document.toDisclosureView(DisclosureLevel.FULL);
 * }</pre>
 */
public enum DisclosureLevel {

    /**
     * Minimal disclosure: ID, title, level only.
     * Best for initial document scanning and structure understanding.
     * Token usage: ~5% of full document.
     */
    OUTLINE(0, "Outline"),

    /**
     * Summary disclosure: Adds summaries and keywords.
     * Best for quick relevance assessment.
     * Token usage: ~15% of full document.
     */
    SUMMARY(1, "Summary"),

    /**
     * Expanded disclosure: Adds metadata and child references.
     * Best for detailed selection decisions.
     * Token usage: ~30% of full document.
     */
    EXPANDED(2, "Expanded"),

    /**
     * Full disclosure: Complete content.
     * Best for final LLM context input.
     * Token usage: 100% of selected chunks.
     */
    FULL(3, "Full");

    private final int level;
    private final String name;

    DisclosureLevel(int level, String name) {
        this.level = level;
        this.name = name;
    }

    /**
     * Returns the numeric level value.
     *
     * @return level value (0=OUTLINE, 1=SUMMARY, 2=EXPANDED, 3=FULL)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the human-readable name.
     *
     * @return display name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this level includes at least the specified detail.
     *
     * @param other the other level to compare against
     * @return true if this level is equal or more detailed
     */
    public boolean includes(DisclosureLevel other) {
        return this.level >= other.level;
    }

    /**
     * Checks if this level is less detailed than the specified level.
     *
     * @param other the other level to compare against
     * @return true if this level is less detailed
     */
    public boolean isLessDetailedThan(DisclosureLevel other) {
        return this.level < other.level;
    }

    /**
     * Gets the next more detailed level.
     *
     * @return the next level, or this if already at FULL
     */
    public DisclosureLevel next() {
        if (this == FULL) {
            return this;
        }
        return values()[this.level + 1];
    }

    /**
     * Gets the next less detailed level.
     *
     * @return the previous level, or this if already at OUTLINE
     */
    public DisclosureLevel previous() {
        if (this == OUTLINE) {
            return this;
        }
        return values()[this.level - 1];
    }

    /**
     * Parses a string to a DisclosureLevel.
     *
     * @param value the string value (case-insensitive)
     * @return the corresponding DisclosureLevel
     * @throws IllegalArgumentException if value is not recognized
     */
    public static DisclosureLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return OUTLINE;
        }
        try {
            return DisclosureLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try matching by name
            for (DisclosureLevel level : values()) {
                if (level.name.equalsIgnoreCase(value.trim())) {
                    return level;
                }
            }
            throw new IllegalArgumentException(
                "Unknown disclosure level: " + value +
                ". Valid values: OUTLINE, SUMMARY, EXPANDED, FULL"
            );
        }
    }
}
