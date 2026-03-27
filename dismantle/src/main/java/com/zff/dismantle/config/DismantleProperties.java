package com.zff.dismantle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Dismantle.
 *
 * <p>Binds YAML configuration to strongly-typed properties.</p>
 *
 * <h3>Example configuration (application.yaml):</h3>
 * <pre>{@code
 * dismantle:
 *   storage:
 *     ttl-minutes: 60
 *     backend: memory  # or redis (future)
 *   chunking:
 *     default-strategy: semantic
 *     semantic:
 *       min-section-length: 50
 *       enable-hierarchical: true
 *   enrichment:
 *     title-generator: rule-based  # or llm
 *     summary-generator: rule-based  # or llm
 *     llm:
 *       provider: ollama
 *       endpoint: http://localhost:11434
 *       model: qwen2.5:7b
 *   retrieval:
 *     default-strategy: keyword
 *     max-results: 10
 *     min-score: 0.3
 * }</pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dismantle")
public class DismantleProperties {

    /**
     * Storage configuration.
     */
    private Storage storage = new Storage();

    /**
     * Chunking configuration.
     */
    private Chunking chunking = new Chunking();

    /**
     * Enrichment configuration.
     */
    private Enrichment enrichment = new Enrichment();

    /**
     * Retrieval configuration.
     */
    private Retrieval retrieval = new Retrieval();

    /**
     * Storage settings.
     */
    @Data
    public static class Storage {
        /**
         * Session TTL in minutes.
         */
        private int ttlMinutes = 60;

        /**
         * Storage backend type (memory, redis).
         */
        private String backend = "memory";
    }

    /**
     * Chunking settings.
     */
    @Data
    public static class Chunking {
        /**
         * Default chunking strategy.
         */
        private String defaultStrategy = "semantic";

        /**
         * Semantic chunker settings.
         */
        private Semantic semantic = new Semantic();

        /**
         * Fixed length chunker settings.
         */
        private Fixed fixed = new Fixed();

        @Data
        public static class Semantic {
            /**
             * Minimum section length to be considered valid.
             */
            private int minSectionLength = 50;

            /**
             * Enable hierarchical chunking.
             */
            private boolean enableHierarchical = true;
        }

        @Data
        public static class Fixed {
            /**
             * Default chunk size in characters.
             */
            private int chunkSize = 1000;

            /**
             * Overlap in characters between chunks.
             */
            private int overlap = 100;
        }
    }

    /**
     * Enrichment settings (title generation, summarization).
     */
    @Data
    public static class Enrichment {
        /**
         * Title generator type (rule-based, llm).
         */
        private String titleGenerator = "rule-based";

        /**
         * Summary generator type (rule-based, llm).
         */
        private String summaryGenerator = "rule-based";

        /**
         * LLM settings.
         */
        private Llm llm = new Llm();

        @Data
        public static class Llm {
            /**
             * LLM provider (ollama).
             */
            private String provider = "ollama";

            /**
             * LLM endpoint URL.
             */
            private String endpoint = "http://localhost:11434";

            /**
             * Model name.
             */
            private String model = "qwen2.5:7b";
        }
    }

    /**
     * Retrieval settings.
     */
    @Data
    public static class Retrieval {
        /**
         * Default retrieval strategy (keyword, semantic, hybrid).
         */
        private String defaultStrategy = "keyword";

        /**
         * Maximum number of results to return.
         */
        private int maxResults = 10;

        /**
         * Minimum score threshold for results.
         */
        private double minScore = 0.3;
    }
}
