package com.zff.dismantle.chunk;

import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.core.HierarchicalChunk;

import java.util.List;

/**
 * Strategy interface for splitting text into chunks.
 *
 * <p>Implementations define different algorithms for dividing text:
 * <ul>
 *   <li>{@link SemanticChunker} - splits by natural boundaries (paragraphs, sections)</li>
 *   <li>{@link FixedLengthChunker} - splits by character count with overlap</li>
 *   <li>{@link OutlineChunker} - extracts only document structure</li>
 *   <li>{@link CompositeChunker} - combines multiple strategies</li>
 * </ul>
 *
 * <p>The newer {@link HierarchicalChunkStrategy} interface supports hierarchical
 * chunking with multiple disclosure levels.
 *
 * @see HierarchicalChunkStrategy
 */
public interface ChunkStrategy {

    /**
     * Split text into flat chunks.
     *
     * @param text the text to split
     * @return list of flat chunks
     */
    List<Chunk> chunk(String text);

    /**
     * Split text into hierarchical chunks with disclosure level control.
     * Default implementation delegates to {@link #chunk(String)} and converts.
     *
     * @param text the text to split
     * @param disclosureLevel the level of detail to include
     * @return list of hierarchical chunks
     */
    default List<HierarchicalChunk> chunkHierarchical(String text, DisclosureLevel disclosureLevel) {
        List<Chunk> flatChunks = chunk(text);
        return flatChunks.stream()
                .map(c -> HierarchicalChunk.builder()
                        .id(c.getId())
                        .content(disclosureLevel == DisclosureLevel.FULL ? c.getContent() : null)
                        .title(c.getTitle())
                        .summary(disclosureLevel.includes(DisclosureLevel.SUMMARY) ? c.getSummary() : null)
                        .index(c.getIndex())
                        .startOffset(c.getStartOffset())
                        .endOffset(c.getEndOffset())
                        .charCount(c.getContent() != null ? c.getContent().length() : 0)
                        .build())
                .toList();
    }
}
