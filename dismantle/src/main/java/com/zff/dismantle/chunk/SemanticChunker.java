package com.zff.dismantle.chunk;

import com.zff.dismantle.ollama.SimpleOllamaClient;
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
                if (!section.trim().isEmpty() && section.trim().length() > 50) { // Skip very short sections
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
        if (text == null || text.isEmpty() || maxSize <= 0) {
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < text.length()) {
            // 1. 确定理论上的最大结束位置
            int end = Math.min(start + maxSize, text.length());

            // 2. 如果不是最后一段，尝试寻找最佳断点
            if (end < text.length()) {
                // 在 [start, end] 范围内寻找标点
                // 注意：lastIndexOf 的第二个参数是包含的，所以直接用 end
                int lastPeriod = text.lastIndexOf('.', end);
                int lastExclamation = text.lastIndexOf('!', end);
                int lastQuestion = text.lastIndexOf('?', end);
                int lastNewline = text.lastIndexOf('\n', end);

                int breakPoint = Math.max(Math.max(lastPeriod, lastExclamation),
                        Math.max(lastQuestion, lastNewline));

                // 只有当找到的标点位置足够靠后（避免切分出太短的片段）才采用
                // 阈值设为 start + maxSize * 0.2 可能比 0.5 更灵活，避免被迫切分长句
                if (breakPoint > start && breakPoint >= start + (maxSize / 3)) {
                    end = breakPoint + 1; // 包含标点
                }
            }

            // 3. 提取片段 (暂时不 trim，保留原始索引对应关系，或在 createChunk 内部处理)
            String segment = text.substring(start, end);

            // 如果片段全是空格，跳过（防止死循环，虽然上面逻辑很难产生全空格）
            if (!segment.trim().isEmpty()) {
                chunks.add(createChunk(segment.trim(), chunkIndex++, start));
            }

            // 4. 计算下一个 start
            // 如果已经到达末尾，直接退出，避免生成纯重叠的尾部片段
            if (end >= text.length()) {
                break;
            }

            // 核心优化：下一个起点 = 当前终点 - 重叠量
            int nextStart = end - overlap;

            // 安全检查：防止死循环（如果 overlap >= 有效长度，必须强制前进）
            if (nextStart <= start) {
                // 极端情况：找不到标点且 overlap 太大，或者句子极短
                // 强制向前移动，至少移动 1 个字符，或者移动 maxSize 的一半
                nextStart = start + Math.max(1, maxSize / 2);
            }

            // 额外优化：尝试让下一个片段的开始也落在句子边界上（可选）
            // 如果 nextStart 落在单词中间，可以稍微调整，但这会增加复杂度，暂保持简单

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
        //todo
        // Extract first meaningful sentence/phrase as title
        SimpleOllamaClient ollamaClient = new SimpleOllamaClient("http://localhost:11434");
        String s = ollamaClient.nameTitle(content);

        System.out.println("index " + index + ",name title:" +s);
        return s.isEmpty() ? "Segment " + (index + 1) : s;
    }
}
