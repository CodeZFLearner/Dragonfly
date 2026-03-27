package com.zff.dismantle.storage;

import com.zff.dismantle.chunk.Chunk;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an analysis session containing chunks from a document.
 * Sessions have a TTL and will expire after a configured duration.
 *
 * <p>This enhanced version supports:
 * <ul>
 *   <li>Document ID reference for V2 hierarchical storage</li>
 *   <li>Backward compatibility with V1 flat chunks</li>
 * </ul>
 */
@Getter
public class AnalysisSession {
    private final String sessionId;
    private final String originalText;
    private final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final Instant createdAt;
    private final Instant expiresAt;
    private int chunkCount = 0;

    /**
     * Reference to V2 document ID.
     */
    @Setter
    private String documentId;

    public AnalysisSession(String sessionId, String originalText, int ttlMinutes) {
        this.sessionId = sessionId;
        this.originalText = originalText;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plusSeconds(ttlMinutes * 60L);
    }

    public void addChunk(Chunk chunk) {
        this.chunks.put(chunk.getId(), chunk);
        this.chunkCount++;
    }

    public Chunk getChunk(String chunkId) {
        return this.chunks.get(chunkId);
    }

    public List<Chunk> getChunks(List<String> chunkIds) {
        return chunkIds.stream()
                .map(this.chunks::get)
                .filter(chunk -> chunk != null)
                .toList();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this session has V1 chunks (for backward compatibility).
     *
     * @return true if V1 chunks exist
     */
    public boolean hasV1Chunks() {
        return !this.chunks.isEmpty();
    }

    /**
     * Gets all V1 chunks.
     *
     * @return map of chunk IDs to chunks
     */
    public Map<String, Chunk> getV1Chunks() {
        return this.chunks;
    }

    /**
     * Gets a V1 chunk by ID.
     *
     * @param chunkId the chunk ID
     * @return the chunk, or null if not found
     */
    public Chunk getV1Chunk(String chunkId) {
        return this.chunks.get(chunkId);
    }
}
