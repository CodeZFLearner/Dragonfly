package com.zff.dismantle.chunk;

import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.core.Document;
import com.zff.dismantle.core.HierarchicalChunk;

import java.util.List;

/**
 * Advanced strategy interface for hierarchical chunking with disclosure level control.
 *
 * <p>This interface extends the basic {@link ChunkStrategy} with support for:
 * <ul>
 *   <li>Hierarchical chunk structures (parent-child relationships)</li>
 *   <li>Progressive disclosure levels</li>
 *   <li>Document-level analysis</li>
 *   <li>Context-aware chunking configuration</li>
 * </ul>
 *
 * <h2>Implementation Priority</h2>
 * When multiple strategies are registered, they are evaluated in priority order.
 * Lower priority values are tried first.
 *
 * <h2>Covariant Return Type</h2>
 * This interface overrides {@link ChunkStrategy#chunk(String)} to return
 * {@code List<HierarchicalChunk>} instead of {@code List<Chunk>}, leveraging
 * Java's covariant return type feature.
 *
 * @see ChunkStrategy
 * @see DisclosureLevel
 */
public interface HierarchicalChunkStrategy extends ChunkStrategy {

    /**
     * Returns the name of this strategy.
     *
     * @return strategy name (e.g., "semantic", "outline", "fixed")
     */
    String getName();

    /**
     * Returns the priority of this strategy.
     * Lower values indicate higher priority.
     *
     * @return priority value
     */
    default int getPriority() {
        return 100; // Default priority
    }

    /**
     * Checks if this strategy supports the given text.
     *
     * @param text the text to check
     * @return true if this strategy can handle the text
     */
    default boolean supports(String text) {
        return text != null && !text.isBlank();
    }

    /**
     * Analyzes and chunks text into a hierarchical document structure.
     *
     * @param text the text to analyze and chunk
     * @return a Document containing the hierarchical chunk structure
     */
    Document analyze(String text);

    /**
     * Analyzes and chunks text with a specific disclosure level.
     *
     * @param text the text to analyze and chunk
     * @param disclosureLevel the level of detail to include in chunks
     * @return a Document containing the hierarchical chunk structure
     */
    default Document analyze(String text, DisclosureLevel disclosureLevel) {
        Document document = analyze(text);
        return switch (disclosureLevel) {
            case OUTLINE -> document.toOutlineView();
            case SUMMARY -> document.toSummaryView();
            case EXPANDED -> document; // Default already expanded
            case FULL -> document.toFullView();
        };
    }

    /**
     * Chunks text into a flat list of hierarchical chunks.
     * <p>
     * This method overrides {@link ChunkStrategy#chunk(String)} with a covariant
     * return type {@code List<HierarchicalChunk>}.
     *
     * @param text the text to chunk
     * @return list of hierarchical chunks
     */
    @Override
    List<HierarchicalChunk> chunk(String text);

    /**
     * Chunks text with a specific disclosure level.
     *
     * @param text the text to chunk
     * @param disclosureLevel the level of detail to include
     * @return list of hierarchical chunks at the specified disclosure level
     */
    default List<HierarchicalChunk> chunk(String text, DisclosureLevel disclosureLevel) {
        List<HierarchicalChunk> chunks = chunk(text);
        return switch (disclosureLevel) {
            case OUTLINE -> chunks.stream().map(HierarchicalChunk::toOutlineView).toList();
            case SUMMARY -> chunks.stream().map(HierarchicalChunk::toSummaryView).toList();
            case EXPANDED -> chunks.stream().map(HierarchicalChunk::toExpandedView).toList();
            case FULL -> chunks.stream().map(HierarchicalChunk::toFullView).toList();
        };
    }

    /**
     * Chunks text into hierarchical chunks with disclosure level control.
     * Alias for {@link #chunk(String, DisclosureLevel)} for backward compatibility.
     *
     * @param text the text to chunk
     * @param disclosureLevel the level of detail to include
     * @return list of hierarchical chunks
     * @deprecated Use {@link #chunk(String, DisclosureLevel)} instead
     */
    @Deprecated
    default List<HierarchicalChunk> chunkHierarchical(String text, DisclosureLevel disclosureLevel) {
        return chunk(text, disclosureLevel);
    }
}
