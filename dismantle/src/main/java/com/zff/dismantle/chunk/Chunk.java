package com.zff.dismantle.chunk;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single chunk of text with its metadata.
 * Content is stored internally and NOT exposed in API responses.
 */
@Data
@Builder
public class Chunk {
    private String id;
    private String content;      // Full original text - NEVER expose in API
    private String title;        // Concise title for API response
    private String summary;      // Internal summary for routing - NOT exposed
    private int index;           // Position in original document
    private int startOffset;     // Character offset in original text
    private int endOffset;       // Character offset in original text
}
