package com.zff.dismantle.chunk;

import java.util.List;

/**
 * Strategy interface for splitting text into chunks.
 */
public interface ChunkStrategy {
    /**
     * Split text into chunks.
     *
     * @param text the text to split
     * @return list of chunks
     */
    List<Chunk> chunk(String text);
}
