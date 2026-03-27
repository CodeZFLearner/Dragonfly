package com.zff.dismantle.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an analyzed document with its hierarchical chunk structure.
 *
 * <p>A Document is the aggregate root that contains:
 * <ul>
 *   <li>Original text content</li>
 *   <li>Hierarchical chunk tree (sections → subsections → paragraphs)</li>
 *   <li>Document-level metadata (title, source, etc.)</li>
 *   <li>Chunk index for fast lookup</li>
 * </ul>
 *
 * <h2>Hierarchical Structure</h2>
 * <pre>
 * Document
 * ├── Section (level=SECTION)
 * │   ├── Subsection (level=SUBSECTION)
 * │   │   └── Paragraph (level=PARAGRAPH)
 * │   └── Paragraph
 * └── Section
 *     └── Paragraph
 * </pre>
 *
 * <h2>Progressive Disclosure</h2>
 * The Document supports multiple disclosure levels:
 * <ul>
 *   <li>{@link #toOutlineView()} - Returns only section titles</li>
 *   <li>{@link #toSummaryView()} - Returns titles + summaries</li>
 *   <li>{@link #toFullView()} - Returns complete structure with content</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * Unique identifier for this document.
     */
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    /**
     * Optional title of the document (may be auto-extracted or user-provided).
     */
    private String title;

    /**
     * The original full text content.
     */
    private String content;

    /**
     * Document-level metadata.
     */
    @Builder.Default
    private ChunkMetadata metadata = new ChunkMetadata();

    /**
     * All chunks in this document (flat list for storage).
     * Hierarchical relationships are maintained via parentId/childIds.
     */
    @Builder.Default
    private Map<String, HierarchicalChunk> chunks = new ConcurrentHashMap<>();

    /**
     * Top-level chunk IDs (roots of the hierarchy).
     */
    @Builder.Default
    private List<String> rootChunkIds = new ArrayList<>();

    /**
     * Total character count of the original document.
     */
    private int charCount;

    /**
     * Estimated token count of the original document.
     */
    private int estimatedTokens;

    /**
     * Creates a new Document from the given text.
     *
     * @param content the original text content
     * @return a new Document instance
     */
    public static Document of(String content) {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .charCount(content != null ? content.length() : 0)
                .estimatedTokens(content != null ? content.length() / 4 : 0)
                .metadata(new ChunkMetadata())
                .chunks(new ConcurrentHashMap<>())
                .rootChunkIds(new ArrayList<>())
                .build();
    }

    /**
     * Creates a new Document from the given text with a title.
     *
     * @param title the document title
     * @param content the original text content
     * @return a new Document instance
     */
    public static Document of(String title, String content) {
        Document doc = of(content);
        doc.setTitle(title);
        return doc;
    }

    /**
     * Adds a chunk to this document.
     *
     * @param chunk the chunk to add
     * @return this Document for chaining
     */
    public Document addChunk(HierarchicalChunk chunk) {
        if (this.chunks == null) {
            this.chunks = new ConcurrentHashMap<>();
        }
        if (this.rootChunkIds == null) {
            this.rootChunkIds = new ArrayList<>();
        }

        this.chunks.put(chunk.getId(), chunk);

        // If no parent, it's a root chunk
        if (chunk.getParentId() == null) {
            this.rootChunkIds.add(chunk.getId());
        } else {
            // Add to parent's children
            HierarchicalChunk parent = this.chunks.get(chunk.getParentId());
            if (parent != null) {
                parent.addChildId(chunk.getId());
            }
        }

        return this;
    }

    /**
     * Gets a chunk by ID.
     *
     * @param chunkId the chunk ID
     * @return the chunk, or null if not found
     */
    public HierarchicalChunk getChunk(String chunkId) {
        return this.chunks != null ? this.chunks.get(chunkId) : null;
    }

    /**
     * Gets all chunks at a specific level.
     *
     * @param level the level to filter by
     * @return list of chunks at that level
     */
    public List<HierarchicalChunk> getChunksByLevel(ChunkLevel level) {
        if (this.chunks == null) {
            return new ArrayList<>();
        }
        return this.chunks.values().stream()
                .filter(chunk -> level == chunk.getLevel())
                .toList();
    }

    /**
     * Gets root chunks (top-level sections).
     *
     * @return list of root chunks
     */
    public List<HierarchicalChunk> getRootChunks() {
        if (this.rootChunkIds == null || this.chunks == null) {
            return new ArrayList<>();
        }
        return this.rootChunkIds.stream()
                .map(this.chunks::get)
                .filter(chunk -> chunk != null)
                .toList();
    }

    /**
     * Gets all child chunks of a given parent.
     *
     * @param parentId the parent chunk ID
     * @return list of child chunks
     */
    public List<HierarchicalChunk> getChildChunks(String parentId) {
        if (this.chunks == null) {
            return new ArrayList<>();
        }
        HierarchicalChunk parent = this.chunks.get(parentId);
        if (parent == null || parent.getChildIds() == null) {
            return new ArrayList<>();
        }
        return parent.getChildIds().stream()
                .map(this.chunks::get)
                .filter(chunk -> chunk != null)
                .toList();
    }

    /**
     * Gets all leaf chunks (paragraphs with no children).
     *
     * @return list of leaf chunks
     */
    public List<HierarchicalChunk> getLeafChunks() {
        if (this.chunks == null) {
            return new ArrayList<>();
        }
        return this.chunks.values().stream()
                .filter(chunk -> !chunk.hasChildren())
                .toList();
    }

    /**
     * Returns the total number of chunks.
     *
     * @return chunk count
     */
    public int getChunkCount() {
        return this.chunks != null ? this.chunks.size() : 0;
    }

    /**
     * Creates an outline view of this document (Level 0 disclosure).
     * Returns only structure (titles) without content.
     *
     * @return a new Document with outline-level detail
     */
    public Document toOutlineView() {
        Document outline = Document.builder()
                .id(this.id)
                .title(this.title)
                .metadata(this.metadata)
                .charCount(this.charCount)
                .estimatedTokens(this.estimatedTokens)
                .chunks(new ConcurrentHashMap<>())
                .rootChunkIds(new ArrayList<>(this.rootChunkIds))
                .build();

        // Add outline views of all chunks
        if (this.chunks != null) {
            for (HierarchicalChunk chunk : this.chunks.values()) {
                outline.chunks.put(chunk.getId(), chunk.toOutlineView());
            }
        }

        return outline;
    }

    /**
     * Creates a summary view of this document (Level 1 disclosure).
     * Returns titles, summaries, and keywords.
     *
     * @return a new Document with summary-level detail
     */
    public Document toSummaryView() {
        Document summary = Document.builder()
                .id(this.id)
                .title(this.title)
                .metadata(this.metadata)
                .charCount(this.charCount)
                .estimatedTokens(this.estimatedTokens)
                .chunks(new ConcurrentHashMap<>())
                .rootChunkIds(new ArrayList<>(this.rootChunkIds))
                .build();

        // Add summary views of all chunks
        if (this.chunks != null) {
            for (HierarchicalChunk chunk : this.chunks.values()) {
                summary.chunks.put(chunk.getId(), chunk.toSummaryView());
            }
        }

        return summary;
    }

    /**
     * Creates a full view of this document (Level 3 disclosure).
     * Returns complete structure with all content.
     *
     * @return a new Document with full detail
     */
    public Document toFullView() {
        Document full = Document.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .metadata(this.metadata)
                .charCount(this.charCount)
                .estimatedTokens(this.estimatedTokens)
                .chunks(new ConcurrentHashMap<>())
                .rootChunkIds(new ArrayList<>(this.rootChunkIds))
                .build();

        // Add full views of all chunks
        if (this.chunks != null) {
            for (HierarchicalChunk chunk : this.chunks.values()) {
                full.chunks.put(chunk.getId(), chunk.toFullView());
            }
        }

        return full;
    }

    /**
     * Checks if this document has chunks.
     *
     * @return true if document has at least one chunk
     */
    public boolean hasChunks() {
        return this.chunks != null && !this.chunks.isEmpty();
    }

    /**
     * Gets a document metadata value.
     *
     * @param key the metadata key
     * @param <T> the expected type
     * @return the value, or null if not present
     */
    public <T> T getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    /**
     * Sets a document metadata value.
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
