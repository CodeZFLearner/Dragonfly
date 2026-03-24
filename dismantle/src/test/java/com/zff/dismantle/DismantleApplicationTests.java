package com.zff.dismantle;

import com.zff.dismantle.chunk.Chunk;
import com.zff.dismantle.chunk.SemanticChunker;
import com.zff.dismantle.chunk.FixedLengthChunker;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DismantleApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private SemanticChunker semanticChunker;

    @Autowired
    private FixedLengthChunker fixedChunker;

    @Test
    void testSemanticChunker() {
        String text = """
                # 第一章 项目背景

                本项目旨在开发一个智能数据分析系统，用于处理大规模的企业数据。
                系统需要具备实时数据处理、可视化展示和智能分析等功能。

                # 第二章 技术方案

                采用微服务架构，使用 Spring Boot 作为基础框架。
                数据存储使用 MySQL 和 Redis 相结合的方式。
                前端采用 Vue.js 框架进行开发。

                # 第三章 实施计划

                项目分为三个阶段：需求分析、系统设计和开发实现。
                每个阶段预计耗时两个月，总计六个月完成。
                """;

        List<Chunk> chunks = semanticChunker.chunk(text);

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 2, "Should split into at least 2 chunks");

        for (Chunk chunk : chunks) {
            assertNotNull(chunk.getId());
            assertNotNull(chunk.getContent());
            assertNotNull(chunk.getTitle());
        }
    }

    @Test
    void testFixedLengthChunker() {
        String text = "这是一段很长的文本，".repeat(100);

        FixedLengthChunker chunker = new FixedLengthChunker(200, 20);
        List<Chunk> chunks = chunker.chunk(text);

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 1);

        for (Chunk chunk : chunks) {
            assertNotNull(chunk.getId());
            assertNotNull(chunk.getContent());
        }
    }

    @Test
    void testEmptyText() {
        List<Chunk> chunks = semanticChunker.chunk("");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testShortText() {
        List<Chunk> chunks = semanticChunker.chunk("这是一段短文本。");
        assertEquals(1, chunks.size());
    }

    public static List<String> extractTextByPage(File pdfFile) throws IOException {
        List<String> pagesContent = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document).trim();
                if (!text.isEmpty()) {
                    pagesContent.add(text);
                }
            }
        }
        return pagesContent;
    }

    @Test
    void pdf(){
        File pdfFile = new File("C:\\Users\\20595\\Desktop\\601689_20260324_GA6O.pdf");
        if (!pdfFile.exists()) {
            System.err.println("文件不存在: " + pdfFile.getAbsolutePath());
            return;
        }

        try {
            System.out.println("📄 正在读取 PDF: " + pdfFile.getName());

            // 方案 A: 读取全文
            // String fullText = extractFullText(pdfFile);
            // System.out.println("全文长度: " + fullText.length());
            // System.out.println("前 200 字符: " + fullText.substring(0, Math.min(200, fullText.length())));

            // 方案 B: 按页读取 (推荐用于长文档)
            List<String> pages = extractTextByPage(pdfFile);

            List<Chunk> chunk = semanticChunker.chunk(String.join("\n\n", pages));
            for (Chunk chunk1 : chunk){
                System.out.println("标题: " + chunk1.getTitle());
//                System.out.println("内容: " + chunk1.getContent().substring(0, Math.min(200, chunk1.getContent().length())) + "...");
                System.out.println("----");
            }
        } catch (IOException e) {
            System.err.println("读取 PDF 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
