package com.zff.dismantle.chunk;

import com.zff.dismantle.core.ChunkLevel;
import com.zff.dismantle.core.DisclosureLevel;
import com.zff.dismantle.core.Document;
import com.zff.dismantle.core.HierarchicalChunk;
import com.zff.dismantle.ollama.SimpleOllamaClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits text by semantic boundaries (paragraphs, sections).
 * Best for documents with natural structure.
 *
 * <p>This enhanced version implements {@link HierarchicalChunkStrategy} for
 * progressive disclosure support. It builds hierarchical chunk structures:
 *
 * <ul>
 *   <li>Level 1 headings → SECTION level chunks</li>
 *   <li>Level 2-3 headings → SUBSECTION level chunks</li>
 *   <li>Paragraphs within sections → PARAGRAPH level chunks</li>
 * </ul>
 *
 * <h2>Chunking Strategy</h2>
 * <ol>
 *   <li>First, split by section headings (Markdown, numbered, Chinese style)</li>
 *   <li>If no clear sections, split by paragraphs</li>
 *   <li>If still too few chunks, split by fixed size with sentence-aware boundaries</li>
 * </ol>
 */
@Component
public class SemanticChunker implements HierarchicalChunkStrategy {

    // Common section/paragraph separators
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(?m)^(#{1,6}\\s+.+)$|^[\\u4e00-\\u9fa5]{2,10} [：:].*$",
            Pattern.MULTILINE
    );

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile(
            "\\n\\s*\\n",
            Pattern.MULTILINE
    );

    private final SimpleOllamaClient ollamaClient;

    public SemanticChunker() {
        this.ollamaClient = new SimpleOllamaClient("http://localhost:11434");
    }

    @Override
    public String getName() {
        return "semantic";
    }

    @Override
    public int getPriority() {
        return 50; // Medium priority
    }

    @Override
    public Document analyze(String text) {
        if (text == null || text.isBlank()) {
            return Document.of("");
        }

        Document document = Document.of(text);
        List<SectionInfo> sections = findSections(text);

        if (sections.isEmpty()) {
            // No clear structure, treat as single section with paragraph chunks
            List<HierarchicalChunk> paragraphChunks = splitByParagraphsHierarchical(text);
            for (HierarchicalChunk chunk : paragraphChunks) {
                document.addChunk(chunk);
            }
            return document;
        }

        // Build hierarchical structure
        buildHierarchicalStructure(document, text, sections);

        return document;
    }

    @Override
    public List<HierarchicalChunk> chunk(String text) {
        Document document = analyze(text);
        return new ArrayList<>(document.getChunks().values());
    }

    @Override
    public List<HierarchicalChunk> chunk(String text, DisclosureLevel disclosureLevel) {
        List<HierarchicalChunk> chunks = chunk(text);
        return switch (disclosureLevel) {
            case OUTLINE -> chunks.stream().map(HierarchicalChunk::toOutlineView).toList();
            case SUMMARY -> chunks.stream().map(HierarchicalChunk::toSummaryView).toList();
            case EXPANDED -> chunks.stream().map(HierarchicalChunk::toExpandedView).toList();
            case FULL -> chunks.stream().map(HierarchicalChunk::toFullView).toList();
        };
    }

    /**
     * Finds all sections in the text with their boundaries.
     */
    private List<SectionInfo> findSections(String text) {
        List<SectionInfo> sections = new ArrayList<>();
        String[] lines = text.split("\\n");

        StringBuilder currentSection = new StringBuilder();
        int sectionStart = 0;
        int currentPos = 0;
        String currentHeading = null;
        ChunkLevel currentLevel = ChunkLevel.PARAGRAPH;

        for (String line : lines) {
            if (isHeading(line)) {
                // Save previous section if exists
                if (currentSection.length() > 0) {
                    sections.add(new SectionInfo(
                            currentHeading,
                            currentLevel,
                            sectionStart,
                            currentPos,
                            currentSection.toString().trim()
                    ));
                }
                // Start new section
                currentHeading = line.trim();
                currentLevel = determineHeadingLevel(line);
                sectionStart = currentPos;
                currentSection = new StringBuilder(line);
            } else {
                currentSection.append("\n").append(line);
            }
            currentPos += line.length() + 1;
        }

        // Add final section
        if (currentSection.length() > 0) {
            sections.add(new SectionInfo(
                    currentHeading,
                    currentLevel,
                    sectionStart,
                    currentPos,
                    currentSection.toString().trim()
            ));
        }

        // Filter out very short sections
        return sections.stream()
                .filter(s -> s.content.length() > 50)
                .toList();
    }

    /**
     * Builds hierarchical chunk structure from sections.
     */
    private void buildHierarchicalStructure(Document document, String text, List<SectionInfo> sections) {
        HierarchicalChunk currentParent = null;

        for (int i = 0; i < sections.size(); i++) {
            SectionInfo section = sections.get(i);
            String chunkId = String.format("sec_%03d", i);

            // Determine if this is a parent (SECTION) or child (SUBSECTION/PARAGRAPH)
            if (section.level == ChunkLevel.SECTION) {
                // This is a top-level section
                HierarchicalChunk sectionChunk = HierarchicalChunk.builder()
                        .id(chunkId)
                        .title(extractTitle(section.heading))
                        .level(section.level)
                        .content(null) // Lazy load
                        .startOffset(section.startOffset)
                        .endOffset(section.endOffset)
                        .charCount(section.content.length())
                        .build();

                document.addChunk(sectionChunk);
                currentParent = sectionChunk;
            } else {
                // This is a subsection or paragraph
                String parentId = currentParent != null ? currentParent.getId() : null;

                HierarchicalChunk childChunk = HierarchicalChunk.builder()
                        .id(chunkId)
                        .title(extractTitle(section.heading))
                        .level(section.level)
                        .content(null) // Lazy load
                        .parentId(parentId)
                        .startOffset(section.startOffset)
                        .endOffset(section.endOffset)
                        .charCount(section.content.length())
                        .build();

                if (currentParent != null) {
                    currentParent.addChildId(chunkId);
                }

                document.addChunk(childChunk);
            }
        }
    }

    /**
     * Splits text into hierarchical paragraph chunks.
     */
    private List<HierarchicalChunk> splitByParagraphsHierarchical(String text) {
        List<HierarchicalChunk> chunks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);

        int index = 0;
        int offset = 0;
        for (String para : paragraphs) {
            if (!para.trim().isEmpty()) {
                String title = generateTitle(para, index);
                HierarchicalChunk chunk = HierarchicalChunk.builder()
                        .id(generateId(index))
                        .title(title)
                        .level(ChunkLevel.PARAGRAPH)
                        .content(para.trim())
                        .startOffset(offset)
                        .endOffset(offset + para.length())
                        .charCount(para.length())
                        .build();
                chunks.add(chunk);
                offset += para.length() + 2; // +2 for \n\n
                index++;
            }
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
        if (text == null || text.isEmpty() || maxSize <= 0) {
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());

            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                int lastExclamation = text.lastIndexOf('!', end);
                int lastQuestion = text.lastIndexOf('?', end);
                int lastNewline = text.lastIndexOf('\n', end);

                int breakPoint = Math.max(Math.max(lastPeriod, lastExclamation),
                        Math.max(lastQuestion, lastNewline));

                if (breakPoint > start && breakPoint >= start + (maxSize / 3)) {
                    end = breakPoint + 1;
                }
            }

            String segment = text.substring(start, end);

            if (!segment.trim().isEmpty()) {
                chunks.add(createChunk(segment.trim(), chunkIndex++, start));
            }

            if (end >= text.length()) {
                break;
            }

            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = start + Math.max(1, maxSize / 2);
            }

            start = nextStart;
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

        if (trimmed.startsWith("#")) {
            return true;
        }

        if (trimmed.matches("^[第第][一二三四五六七八九十\\d]+[章部分节].*")) {
            return true;
        }

        if (trimmed.matches("^\\d+\\.\\d*\\s+.*")) {
            return true;
        }

        if (trimmed.length() < 50 && trimmed.matches("^[A-Z\\u4e00-\\u9fa5].*")) {
            return true;
        }

        return false;
    }

    private ChunkLevel determineHeadingLevel(String line) {
        String trimmed = line.trim();

        if (trimmed.startsWith("#")) {
            int hashCount = 0;
            for (char c : trimmed.toCharArray()) {
                if (c == '#') hashCount++;
                else break;
            }
            if (hashCount == 1) return ChunkLevel.SECTION;
            if (hashCount <= 3) return ChunkLevel.SUBSECTION;
            return ChunkLevel.PARAGRAPH;
        }

        if (trimmed.matches("^[第第][一二三四五六七八九十\\d]+[章部分节].*")) {
            return ChunkLevel.SECTION;
        }

        if (trimmed.matches("^\\d+\\.\\s+.*")) {
            return ChunkLevel.SECTION;
        }

        if (trimmed.matches("^\\d+\\.\\d+.*")) {
            return ChunkLevel.SUBSECTION;
        }

        return ChunkLevel.PARAGRAPH;
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
        if (firstLine.length() > 80) {
            // Try to find a natural break point
            int breakPoint = firstLine.indexOf(' ', 60);
            if (breakPoint > 0) {
                firstLine = firstLine.substring(0, breakPoint) + "...";
            } else {
                firstLine = firstLine.substring(0, 77) + "...";
            }
        }
        return firstLine.isEmpty() ? "Segment " + (index + 1) : firstLine;
    }

    private String extractTitle(String heading) {
        if (heading == null || heading.isBlank()) {
            return "Untitled Section";
        }

        String trimmed = heading.trim();

        // Remove Markdown hashes
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.replaceAll("^#+\\s*", "");
        }

        // Limit length
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 97) + "...";
        }

        return trimmed;
    }

    /**
     * Internal class to represent a section with its boundaries.
     */
    private static class SectionInfo {
        final String heading;
        final ChunkLevel level;
        final int startOffset;
        final int endOffset;
        final String content;

        SectionInfo(String heading, ChunkLevel level, int startOffset, int endOffset, String content) {
            this.heading = heading;
            this.level = level;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.content = content;
        }
    }
}
