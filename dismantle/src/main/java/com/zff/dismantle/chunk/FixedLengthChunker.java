package com.zff.dismantle.chunk;

import com.zff.dismantle.core.ChunkLevel;
import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.core.Document;
import com.zff.dismantle.core.HierarchicalChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits text by fixed character count with overlap.
 * Useful for text without clear structure.
 *
 * <p>This enhanced version implements {@link HierarchicalChunkStrategy} for
 * progressive disclosure support.</p>
 */
@Component
public class FixedLengthChunker implements HierarchicalChunkStrategy {

    private final int chunkSize;
    private final int overlap;

    public FixedLengthChunker() {
        this(1000, 100);  // Default: 1000 chars per chunk, 100 chars overlap
    }

    public FixedLengthChunker(int chunkSize, int overlap) {
        this.chunkSize = Math.max(100, chunkSize);  // Ensure minimum chunk size
        this.overlap = Math.max(0, Math.min(overlap, chunkSize / 2));  // Ensure overlap is reasonable
    }

    @Override
    public String getName() {
        return "fixed";
    }

    @Override
    public int getPriority() {
        return 100; // Lower priority than semantic/outline
    }

    @Override
    public Document analyze(String text) {
        if (text == null || text.isEmpty()) {
            return Document.of("");
        }

        Document document = Document.of(text);
        List<HierarchicalChunk> chunks = chunkHierarchical(text, DisclosureLevel.FULL);

        for (HierarchicalChunk chunk : chunks) {
            document.addChunk(chunk);
        }

        return document;
    }

    @Override
    public List<HierarchicalChunk> chunk(String text) {
        return chunkHierarchical(text, DisclosureLevel.FULL);
    }

    @Override
    public List<HierarchicalChunk> chunkHierarchical(String text, DisclosureLevel disclosureLevel) {
        List<HierarchicalChunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int index = 0;

        while (start < text.length()) {
            // 1. Determine theoretical end position
            int end = Math.min(start + chunkSize, text.length());

            // 2. Try to break at sentence boundary (not for last chunk)
            if (end < text.length()) {
                // Look for sentence endings within the chunk
                int lastPeriod = findLastChar(text, '.', start, end);
                int lastExclamation = findLastChar(text, '!', start, end);
                int lastQuestion = findLastChar(text, '?', start, end);
                int lastNewline = findLastChar(text, '\n', start, end);

                int breakPoint = Math.max(
                        Math.max(lastPeriod, lastExclamation),
                        Math.max(lastQuestion, lastNewline)
                );

                // Only use break point if it's in the second half of the chunk
                int minBreakPoint = start + chunkSize / 3;
                if (breakPoint > minBreakPoint) {
                    end = breakPoint + 1; // Include the punctuation
                }
            }

            // 3. Extract and trim segment
            String segment = text.substring(start, end).trim();

            if (!segment.isEmpty()) {
                HierarchicalChunk chunk = createHierarchicalChunk(segment, index, start, end, disclosureLevel);
                chunks.add(chunk);
                index++;
            }

            // 4. Calculate next start position with overlap
            int nextStart = end - overlap;

            // Safety check: prevent infinite loop
            if (nextStart <= start) {
                // Force前进 if overlap would cause backtracking
                nextStart = end;
            }

            start = nextStart;

            // Exit if we've reached the end
            if (start >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    /**
     * Finds the last occurrence of a character in a range.
     *
     * @param text the text to search
     * @param ch the character to find
     * @param start start index (inclusive)
     * @param end end index (exclusive)
     * @return index of last occurrence, or -1 if not found
     */
    private int findLastChar(String text, char ch, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (text.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }

    private HierarchicalChunk createHierarchicalChunk(
            String content,
            int index,
            int startOffset,
            int endOffset,
            DisclosureLevel disclosureLevel
    ) {
        String title = generateTitle(content, index);
        String summary = content.substring(0, Math.min(100, content.length()));

        // Apply disclosure level
        String contentToReturn = disclosureLevel == DisclosureLevel.FULL ? content : null;

        return HierarchicalChunk.builder()
                .id(generateId(index))
                .content(contentToReturn)
                .title(title)
                .summary(disclosureLevel.includes(DisclosureLevel.SUMMARY) ? summary : null)
                .level(ChunkLevel.PARAGRAPH)
                .index(index)
                .startOffset(startOffset)
                .endOffset(endOffset)
                .charCount(content.length())
                .build();
    }

    private String generateId(int index) {
        return String.format("fix_%03d", index);
    }

    private String generateTitle(String content, int index) {
        if (content == null || content.isEmpty()) {
            return "Segment " + (index + 1);
        }

        String firstLine = content.split("\\n")[0].trim();

        if (firstLine.isEmpty()) {
            return "Segment " + (index + 1);
        }

        if (firstLine.length() > 60) {
            // Try to find a natural break point
            int breakPoint = firstLine.lastIndexOf(' ', Math.min(55, firstLine.length() - 1));
            if (breakPoint > 30) {
                firstLine = firstLine.substring(0, breakPoint) + "...";
            } else {
                firstLine = firstLine.substring(0, 57) + "...";
            }
        }

        return firstLine;
    }
}
