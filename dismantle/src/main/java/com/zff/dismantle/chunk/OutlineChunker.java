package com.zff.dismantle.chunk;

import com.zff.dismantle.core.ChunkLevel;
import com.zff.dismantle.core.ChunkMetadata;
import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.core.Document;
import com.zff.dismantle.core.HierarchicalChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts document outline structure without full content.
 *
 * <p>This strategy identifies headings and section markers to build a
 * hierarchical table of contents. It's ideal for Level 0 (OUTLINE)
 * progressive disclosure, returning minimal tokens while preserving
 * document structure.
 *
 * <h2>Supported Heading Formats</h2>
 * <ul>
 *   <li>Markdown: {@code # Heading}, {@code ## Subheading}</li>
 *   <li>Numbered: {@code 1. Title}, {@code 1.1 Subtitle}</li>
 *   <li>Chinese: {@code 第一章}, {@code 一、}, {@code （一）}</li>
 *   <li>Roman: {@code I.}, {@code II.}, {@code i.}</li>
 * </ul>
 *
 * <h2>Output</h2>
 * Returns a {@link Document} with hierarchical chunks at SECTION,
 * SUBSECTION, and PARAGRAPH levels. Full content is NOT loaded
 * until explicitly requested (lazy loading).
 */
@Component
public class OutlineChunker implements HierarchicalChunkStrategy {

    /**
     * Pattern for Markdown-style headings: # Heading
     */
    private static final Pattern MARKDOWN_HEADING = Pattern.compile(
            "^(#{1,6})\\s+(.+)$",
            Pattern.MULTILINE
    );

    /**
     * Pattern for numbered headings: 1.1 Title
     */
    private static final Pattern NUMBERED_HEADING = Pattern.compile(
            "^(\\d+(?:\\.\\d+)*)\\s+(.+)$",
            Pattern.MULTILINE
    );

    /**
     * Pattern for Chinese-style headings: 第一章，一、
     */
    private static final Pattern CHINESE_HEADING = Pattern.compile(
            "^[第]?[一二三四五六七八九十百千\\d]+[章部分节编卷]|^[（(][一二三四五六七八九十\\d+[）)]].*",
            Pattern.MULTILINE
    );

    /**
     * Pattern to detect any heading line
     */
    private static final Pattern HEADING_LINE = Pattern.compile(
            "^(#{1,6}\\s+.+|\\d+(?:\\.\\d+)*\\s+.+|[第第]?[一二三四五六七八九十\\d]+[章部分节].+)$",
            Pattern.MULTILINE
    );

    @Override
    public String getName() {
        return "outline";
    }

    @Override
    public int getPriority() {
        return 10; // High priority for outline extraction
    }

    @Override
    public Document analyze(String text) {
        if (text == null || text.isBlank()) {
            return Document.of("");
        }

        Document document = Document.of(text);
        List<HeadingOccurrence> headings = findHeadings(text);

        if (headings.isEmpty()) {
            // No headings found, treat entire text as one section
            HierarchicalChunk rootChunk = createChunk(
                    "section_001",
                    "Document Content",
                    ChunkLevel.SECTION,
                    text,
                    0,
                    text.length()
            );
            document.addChunk(rootChunk);
            return document;
        }

        // Build hierarchical structure from headings
        buildHierarchicalStructure(document, text, headings);

        return document;
    }

    @Override
    public List<HierarchicalChunk> chunk(String text) {
        Document document = analyze(text);
        return new ArrayList<>(document.getChunks().values());
    }

    /**
     * Finds all headings in the text with their positions and levels.
     *
     * @param text the text to analyze
     * @return list of heading occurrences
     */
    private List<HeadingOccurrence> findHeadings(String text) {
        List<HeadingOccurrence> headings = new ArrayList<>();
        String[] lines = text.split("\\n");

        int currentPos = 0;
        int index = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                currentPos += line.length() + 1; // +1 for newline
                continue;
            }

            HeadingOccurrence heading = parseHeading(trimmed, currentPos, index++);
            if (heading != null) {
                headings.add(heading);
            }

            currentPos += line.length() + 1;
        }

        return headings;
    }

    /**
     * Parses a single line to check if it's a heading.
     *
     * @param line the line to parse
     * @param position character position in original text
     * @param index heading index
     * @return HeadingOccurrence if it's a heading, null otherwise
     */
    private HeadingOccurrence parseHeading(String line, int position, int index) {
        // Markdown headings
        Matcher mdMatcher = MARKDOWN_HEADING.matcher(line);
        if (mdMatcher.matches()) {
            String hashes = mdMatcher.group(1);
            String title = mdMatcher.group(2).trim();
            int level = hashes.length(); // # = 1, ## = 2, etc.
            return new HeadingOccurrence(title, toChunkLevel(level), position, index);
        }

        // Numbered headings
        Matcher numMatcher = NUMBERED_HEADING.matcher(line);
        if (numMatcher.matches()) {
            String numbers = numMatcher.group(1);
            String title = numMatcher.group(2).trim();
            int dots = numbers.split("\\.").length - 1;
            int level = dots + 1;
            return new HeadingOccurrence(title, toChunkLevel(level), position, index);
        }

        // Chinese-style headings (treat as level 1 or 2)
        if (CHINESE_HEADING.matcher(line).matches()) {
            return new HeadingOccurrence(line.trim(), ChunkLevel.SECTION, position, index);
        }

        // Short all-caps or title-like lines
        if (line.length() < 80 &&
            !line.endsWith(".") &&
            !line.endsWith(":") &&
            Character.isUpperCase(line.charAt(0))) {
            // Could be a title, but lower confidence
            // Only consider if it's on its own line
            return new HeadingOccurrence(line.trim(), ChunkLevel.SUBSECTION, position, index);
        }

        return null;
    }

    /**
     * Converts Markdown heading level to ChunkLevel.
     *
     * @param mdLevel Markdown level (1-6)
     * @return corresponding ChunkLevel
     */
    private ChunkLevel toChunkLevel(int mdLevel) {
        return switch (mdLevel) {
            case 1 -> ChunkLevel.SECTION;
            case 2, 3 -> ChunkLevel.SUBSECTION;
            default -> ChunkLevel.PARAGRAPH;
        };
    }

    /**
     * Builds hierarchical chunk structure from heading occurrences.
     *
     * @param document the document to populate
     * @param text the original text
     * @param headings list of heading occurrences
     */
    private void buildHierarchicalStructure(Document document, String text, List<HeadingOccurrence> headings) {
        for (int i = 0; i < headings.size(); i++) {
            HeadingOccurrence current = headings.get(i);
            HeadingOccurrence next = (i + 1 < headings.size()) ? headings.get(i + 1) : null;

            // Calculate section boundaries
            int startOffset = current.position;
            int endOffset = (next != null) ? next.position : text.length();

            // Extract section text (for potential lazy loading)
            String sectionText = text.substring(startOffset, endOffset).trim();

            // Create chunk ID
            String chunkId = String.format("sec_%03d", current.index);

            // Find parent chunk
            String parentId = findParentChunkId(headings, i, document);

            // Create the chunk
            HierarchicalChunk chunk = createChunk(
                    chunkId,
                    current.title,
                    current.level,
                    null, // Lazy load content
                    startOffset,
                    endOffset
            );
            chunk.setParentId(parentId);
            chunk.setCharCount(sectionText.length());

            // Add minimal metadata
            ChunkMetadata metadata = new ChunkMetadata();
            metadata.put("headingType", getHeadingType(current.title));
            chunk.setMetadata(metadata);

            document.addChunk(chunk);
        }
    }

    /**
     * Finds the parent chunk ID for a heading based on hierarchy.
     *
     * @param headings all headings
     * @param currentIndex current heading index
     * @param document the document being built
     * @return parent chunk ID or null if root level
     */
    private String findParentChunkId(List<HeadingOccurrence> headings, int currentIndex, Document document) {
        HeadingOccurrence current = headings.get(currentIndex);

        // Look backwards for a heading with lower level (parent)
        for (int i = currentIndex - 1; i >= 0; i--) {
            HeadingOccurrence potentialParent = headings.get(i);
            if (potentialParent.level.getDepth() < current.level.getDepth()) {
                // Found potential parent
                String parentId = String.format("sec_%03d", potentialParent.index);
                if (document.getChunk(parentId) != null) {
                    return parentId;
                }
            }
        }

        return null; // Root level
    }

    /**
     * Creates a hierarchical chunk with the given properties.
     *
     * @param id chunk ID
     * @param title chunk title
     * @param level chunk level
     * @param content chunk content (can be null for lazy loading)
     * @param startOffset start offset in original text
     * @param endOffset end offset in original text
     * @return new HierarchicalChunk instance
     */
    private HierarchicalChunk createChunk(
            String id,
            String title,
            ChunkLevel level,
            String content,
            int startOffset,
            int endOffset
    ) {
        return HierarchicalChunk.builder()
                .id(id)
                .title(title)
                .level(level)
                .content(content)
                .startOffset(startOffset)
                .endOffset(endOffset)
                .charCount(endOffset - startOffset)
                .build();
    }

    /**
     * Determines the type of heading based on its format.
     *
     * @param title the heading text
     * @return heading type string
     */
    private String getHeadingType(String title) {
        if (title.startsWith("#")) {
            return "MARKDOWN";
        }
        if (title.matches("^\\d+\\..*")) {
            return "NUMBERED";
        }
        if (title.matches("^[第第]?.*")) {
            return "CHINESE";
        }
        return "PLAIN";
    }

    /**
     * Internal class to represent a heading occurrence.
     */
    private static class HeadingOccurrence {
        final String title;
        final ChunkLevel level;
        final int position;
        final int index;

        HeadingOccurrence(String title, ChunkLevel level, int position, int index) {
            this.title = title;
            this.level = level;
            this.position = position;
            this.index = index;
        }
    }
}
