package com.zff.dismantle.chunk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits text by fixed character count with overlap.
 * Useful for text without clear structure.
 */
@Component
public class FixedLengthChunker implements ChunkStrategy {

    private final int chunkSize;
    private final int overlap;

    public FixedLengthChunker() {
        this(1000, 100);  // Default: 1000 chars per chunk, 100 chars overlap
    }

    public FixedLengthChunker(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<Chunk> chunk(String text) {
        List<Chunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at word/sentence boundary
            if (end < text.length()) {
                // Look for sentence endings
                int lastPeriod = text.lastIndexOf('.', end);
                int lastExclamation = text.lastIndexOf('!', end);
                int lastQuestion = text.lastIndexOf('?', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int lastSpace = text.lastIndexOf(' ', end);

                int breakPoint = Math.max(
                        Math.max(lastPeriod, lastExclamation),
                        Math.max(lastQuestion, Math.max(lastNewline, lastSpace))
                );

                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 1;
                }
            }

            String segment = text.substring(start, end).trim();
            if (!segment.isEmpty()) {
                chunks.add(createChunk(segment, index++, start));
            }

            // Move start with overlap
            start = end - overlap;
            if (start >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    private Chunk createChunk(String content, int index, int startOffset) {
        String title = generateTitle(content, index);
        return Chunk.builder()
                .id(generateId(index))
                .content(content)
                .title(title)
                .summary(content.substring(0, Math.min(100, content.length())))
                .index(index)
                .startOffset(startOffset)
                .endOffset(startOffset + content.length())
                .build();
    }

    private String generateId(int index) {
        return String.format("chunk_%03d", index);
    }

    private String generateTitle(String content, int index) {
        String firstLine = content.split("\\n")[0].trim();
        if (firstLine.length() > 50) {
            firstLine = firstLine.substring(0, 47) + "...";
        }
        return firstLine.isEmpty() ? "Segment " + (index + 1) : firstLine;
    }
}
