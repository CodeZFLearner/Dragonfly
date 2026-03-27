package com.zff.dismantle.core;

/**
 * Represents the granularity level of a chunk in hierarchical document structure.
 *
 * <p>Progressive Disclosure uses levels to control how much detail is revealed:
 * <ul>
 *   <li>{@code DOCUMENT} - Top-level document metadata</li>
 *   <li>{@code SECTION} - Major sections (chapters, parts)</li>
 *   <li>{@code SUBSECTION} - Sub-sections within sections</li>
 *   <li>{@code PARAGRAPH} - Individual paragraphs or content blocks</li>
 * </ul>
 */
public enum ChunkLevel {

    /**
     * Document-level chunk representing the entire document.
     */
    DOCUMENT(0),

    /**
     * Section-level chunk (e.g., chapter, major division).
     */
    SECTION(1),

    /**
     * Subsection-level chunk (e.g., sub-chapter, minor division).
     */
    SUBSECTION(2),

    /**
     * Paragraph-level chunk (finest granularity).
     */
    PARAGRAPH(3);

    private final int depth;

    ChunkLevel(int depth) {
        this.depth = depth;
    }

    /**
     * Returns the depth of this level in the hierarchy.
     * 0 = root (document), higher = more detailed.
     *
     * @return depth value
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Checks if this level is more detailed than the given level.
     *
     * @param other the other level to compare
     * @return true if this level is more detailed (higher depth)
     */
    public boolean isMoreDetailedThan(ChunkLevel other) {
        return this.depth > other.depth;
    }

    /**
     * Checks if this level is less detailed than the given level.
     *
     * @param other the other level to compare
     * @return true if this level is less detailed (lower depth)
     */
    public boolean isLessDetailedThan(ChunkLevel other) {
        return this.depth < other.depth;
    }
}
