package com.zff.dismantle.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a hierarchical chunk of text with metadata.
 *
 * <p>Unlike the flat {@link Chunk}, this class supports:
 * <ul>
 *   <li>Parent-child relationships for document hierarchy</li>
 *   <li>Multiple disclosure levels (outline → summary → full content)</li>
 *   <li>Extensible metadata via {@link ChunkMetadata}</li>
 *   <li>Lazy loading of full content</li>
 * </ul>
 *
 * <h2>Progressive Disclosure Pattern</h2>
 * <pre>
 * Level 0 (OUTLINE): Return only id, title, level
 * Level 1 (SUMMARY): Add summary, keywords
 * Level 2 (EXPANDED): Add detailed summary, child chunks
 * Level 3 (FULL): Return complete content
 * </pre>
 *
 * @see ChunkLevel
 * @see ChunkMetadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchicalChunk {

    /**
     * Unique identifier for this chunk.
     */
    private String id;

    /**
     * The full original text content.
     * May be null until explicitly requested (lazy loading).
     */
    private String content;

    /**
     * Concise title for display and routing.
     */
    private String title;

    /**
     * The granularity level of this chunk.
     */
    private ChunkLevel level;

    /**
     * ID of the parent chunk (null for top-level chunks).
     */
    private String parentId;

    /**
     * IDs of child chunks (empty for leaf chunks).
     */
    @Builder.Default
    private List<String> childIds = new ArrayList<>();

    /**
     * Optional AI-generated or extracted summary.
     */
    private String summary;

    /**
     * Extensible metadata container.
     */
    @Builder.Default
    private ChunkMetadata metadata = new ChunkMetadata();

    /**
     * Position in the original document (0-based).
     */
    private int index;

    /**
     * Character offset where this chunk starts in the original text.
     */
    private int startOffset;

    /**
     * Character offset where this chunk ends in the original text.
     */
    private int endOffset;

    /**
     * Estimated character count for token calculation.
     */
    private int charCount;

    /**
     * Creates a minimal view of this chunk for Level 0 (OUTLINE) disclosure.
     * Contains only id, title, and level.
     *
     * @return a new HierarchicalChunk with minimal fields
     */
    public HierarchicalChunk toOutlineView() {
        return HierarchicalChunk.builder()
                .id(this.id)
                .title(this.title)
                .level(this.level)
                .parentId(this.parentId)
                .childIds(this.childIds)
                .charCount(this.charCount)
                .build();
    }

    /**
     * Creates a summary view of this chunk for Level 1 (SUMMARY) disclosure.
     * Contains id, title, level, summary, and keywords (if available).
     *
     * @return a new HierarchicalChunk with summary-level fields
     */
    public HierarchicalChunk toSummaryView() {
        HierarchicalChunk.HierarchicalChunkBuilder builder = HierarchicalChunk.builder()
                .id(this.id)
                .title(this.title)
                .level(this.level)
                .summary(this.summary)
                .parentId(this.parentId)
                .childIds(this.childIds)
                .charCount(this.charCount);

        // Include keywords if available
        if (this.metadata != null && this.metadata.contains("keywords")) {
            builder.metadata(this.metadata);
        }

        return builder.build();
    }

    /**
     * Creates an expanded view of this chunk for Level 2 (EXPANDED) disclosure.
     * Contains all summary fields plus child chunk references.
     *
     * @return a new HierarchicalChunk with expanded fields
     */
    public HierarchicalChunk toExpandedView() {
        return HierarchicalChunk.builder()
                .id(this.id)
                .title(this.title)
                .level(this.level)
                .summary(this.summary)
                .parentId(this.parentId)
                .childIds(this.childIds)
                .metadata(this.metadata)
                .charCount(this.charCount)
                .index(this.index)
                .startOffset(this.startOffset)
                .endOffset(this.endOffset)
                .build();
    }

    /**
     * Creates a full view of this chunk for Level 3 (FULL) disclosure.
     * Contains all fields including the complete content.
     *
     * @return a new HierarchicalChunk with all fields
     */
    public HierarchicalChunk toFullView() {
        return HierarchicalChunk.builder()
                .id(this.id)
                .content(this.content)
                .title(this.title)
                .level(this.level)
                .parentId(this.parentId)
                .childIds(this.childIds)
                .summary(this.summary)
                .metadata(this.metadata)
                .index(this.index)
                .startOffset(this.startOffset)
                .endOffset(this.endOffset)
                .charCount(this.charCount)
                .build();
    }

    /**
     * Checks if this chunk has child chunks.
     *
     * @return true if this chunk has children
     */
    public boolean hasChildren() {
        return this.childIds != null && !this.childIds.isEmpty();
    }

    /**
     * Adds a child chunk ID.
     *
     * @param childId the child chunk ID to add
     */
    public void addChildId(String childId) {
        if (this.childIds == null) {
            this.childIds = new ArrayList<>();
        }
        this.childIds.add(childId);
    }

    /**
     * Checks if content is loaded (not null).
     *
     * @return true if content is available
     */
    public boolean hasContent() {
        return this.content != null && !this.content.isEmpty();
    }

    /**
     * Checks if summary is available.
     *
     * @return true if summary is available
     */
    public boolean hasSummary() {
        return this.summary != null && !this.summary.isEmpty();
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @param <T> the expected type
     * @return the value, or null if not present
     */
    public <T> T getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    /**
     * Sets a metadata value.
     *
     * @param key the metadata key
     * @param value the value
     */
    public void putMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new ChunkMetadata();
        }
        this.metadata.put(key, value);
    }
}
