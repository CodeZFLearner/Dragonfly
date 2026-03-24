package com.zff.dismantle.chunk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Splits text by semantic boundaries (paragraphs, sections).
 * Best for documents with natural structure.
 */
@Component
public class SemanticChunker implements ChunkStrategy {

    // Common section/paragraph separators
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(?m)^(#{1,6}\\s+.+)$|^[\\u4e00-\\u9fa5]{2,10} [：:].*$",
            Pattern.MULTILINE
    );

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile(
            "\\n\\s*\\n",
            Pattern.MULTILINE
    );

    @Override
    public List<Chunk> chunk(String text) {
        List<Chunk> chunks = new ArrayList<>();

        // First try to split by sections (headings)
        List<String> sections = splitBySections(text);

        if (sections.size() >= 2) {
            // Has clear section structure
            int index = 0;
            int offset = 0;
            for (String section : sections) {
                if (!section.trim().isEmpty()) {
                    chunks.add(createChunk(section.trim(), index++, offset));
                    offset += section.length();
                }
            }
        } else {
            // Fall back to paragraph-based splitting
            chunks = splitByParagraphs(text);
        }

        // If still too few chunks, split by fixed size
        if (chunks.size() < 2 && text.length() > 500) {
            return splitBySize(text, 1000, 100);
        }

        return chunks;
    }

    private List<String> splitBySections(String text) {
        List<String> sections = new ArrayList<>();
        String[] lines = text.split("\\n");

        StringBuilder currentSection = new StringBuilder();
        for (String line : lines) {
            if (isHeading(line) || currentSection.isEmpty()) {
                if (!currentSection.isEmpty()) {
                    sections.add(currentSection.toString());
                }
                currentSection = new StringBuilder(line);
            } else {
                currentSection.append("\n").append(line);
            }
        }
        if (!currentSection.isEmpty()) {
            sections.add(currentSection.toString());
        }

        return sections.isEmpty() ? List.of(text) : sections;
    }

    private List<Chunk> splitByParagraphs(String text) {
        List<Chunk> chunks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);

        int index = 0;
        int offset = 0;
        for (String para : paragraphs) {
            if (!para.trim().isEmpty()) {
                chunks.add(createChunk(para.trim(), index++, offset));
                offset += para.length() + 2; // +2 for \n\n
            }
        }

        // Merge small chunks
        return mergeSmallChunks(chunks);
    }

    private List<Chunk> splitBySize(String text, int maxSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());

            // Try to break at sentence boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                int lastExclamation = text.lastIndexOf('!', end);
                int lastQuestion = text.lastIndexOf('?', end);
                int lastNewline = text.lastIndexOf('\n', end);

                int breakPoint = Math.max(Math.max(lastPeriod, lastExclamation),
                        Math.max(lastQuestion, lastNewline));

                if (breakPoint > start + maxSize / 2) {
                    end = breakPoint + 1;
                }
            }

            String segment = text.substring(start, end).trim();
            if (!segment.isEmpty()) {
                chunks.add(createChunk(segment, index++, start));
            }

            start = end - overlap;
            if (start >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    private List<Chunk> mergeSmallChunks(List<Chunk> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<Chunk> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int startIndex = chunks.get(0).getStartOffset();
        int minChunkSize = 200;

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            if (current.length() + chunk.getContent().length() < minChunkSize && i < chunks.size() - 1) {
                current.append(chunk.getContent()).append("\n\n");
            } else {
                if (current.length() > 0) {
                    current.append(chunk.getContent());
                    merged.add(createChunk(current.toString(), merged.size(), startIndex));
                    current = new StringBuilder();
                    startIndex = chunk.getStartOffset();
                } else {
                    merged.add(chunk);
                }
            }
        }

        if (current.length() > 0) {
            merged.add(createChunk(current.toString(), merged.size(), startIndex));
        }

        return merged.isEmpty() ? chunks : merged;
    }

    private boolean isHeading(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String trimmed = line.trim();

        // Markdown headings: # Heading
        if (trimmed.startsWith("#")) {
            return true;
        }

        // Chinese style headings: 第一章 xxx, 一、xxx
        if (trimmed.matches("^[第第][一二三四五六七八九十\\d]+[章部分节].*")) {
            return true;
        }

        // Numbered headings: 1. xxx, 1.1 xxx
        if (trimmed.matches("^\\d+\\.\\d*\\s+.*")) {
            return true;
        }

        // All caps or very short lines that look like titles
        if (trimmed.length() < 50 && trimmed.matches("^[A-Z\\u4e00-\\u9fa5].*")) {
            return true;
        }

        return false;
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
        // Extract first meaningful sentence/phrase as title
        String firstLine = content.split("\\n")[0].trim();

        if (firstLine.length() > 50) {
            firstLine = firstLine.substring(0, 47) + "...";
        }

        // If starts with heading pattern, use it
        if (firstLine.startsWith("#")) {
            return firstLine.replace("#", "").trim();
        }

        return firstLine.isEmpty() ? "Segment " + (index + 1) : firstLine;
    }
}
