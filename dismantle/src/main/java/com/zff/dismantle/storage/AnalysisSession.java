package com.zff.dismantle.storage;

import com.zff.dismantle.chunk.Chunk;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an analysis session containing chunks from a document.
 * Sessions have a TTL and will expire after a configured duration.
 */
@Getter
public class AnalysisSession {
    private final String sessionId;
    private final String originalText;
    private final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final Instant createdAt;
    private final Instant expiresAt;
    private int chunkCount = 0;

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
}
