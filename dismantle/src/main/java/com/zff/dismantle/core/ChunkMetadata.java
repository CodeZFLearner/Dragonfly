package com.zff.dismantle.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Extensible metadata container for chunks.
 *
 * <p>Supports arbitrary key-value pairs for future extensibility:
 * <ul>
 *   <li>keywords - for keyword-based retrieval</li>
 *   <li>embeddings - for vector search (future)</li>
 *   <li>entities - extracted named entities</li>
 *   <li>sentiment - sentiment analysis result</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMetadata {

    /**
     * Internal storage for metadata entries.
     */
    private Map<String, Object> entries;

    /**
     * Creates a new empty metadata instance.
     */
    public ChunkMetadata() {
        this.entries = new HashMap<>();
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @param <T> the expected type
     * @return the value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return entries != null ? (T) entries.get(key) : null;
    }

    /**
     * Gets a metadata value or returns a default if not present.
     *
     * @param key the metadata key
     * @param defaultValue the default value
     * @param <T> the expected type
     * @return the value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return entries != null ? (T) entries.getOrDefault(key, defaultValue) : defaultValue;
    }

    /**
     * Sets a metadata value.
     *
     * @param key the metadata key
     * @param value the value
     * @return this instance for chaining
     */
    public ChunkMetadata put(String key, Object value) {
        if (this.entries == null) {
            this.entries = new HashMap<>();
        }
        this.entries.put(key, value);
        return this;
    }

    /**
     * Checks if a metadata key exists.
     *
     * @param key the key to check
     * @return true if present
     */
    public boolean contains(String key) {
        return entries != null && entries.containsKey(key);
    }

    /**
     * Gets all metadata keys.
     *
     * @return set of keys
     */
    public java.util.Set<String> keys() {
        return entries != null ? entries.keySet() : java.util.Collections.emptySet();
    }

    /**
     * Merges another metadata into this one.
     *
     * @param other the other metadata to merge
     * @return this instance for chaining
     */
    public ChunkMetadata merge(ChunkMetadata other) {
        if (other == null || other.entries == null) {
            return this;
        }
        if (this.entries == null) {
            this.entries = new HashMap<>(other.entries);
        } else {
            this.entries.putAll(other.entries);
        }
        return this;
    }

    /**
     * Creates a shallow copy of this metadata.
     *
     * @return a new instance with the same entries
     */
    public ChunkMetadata copy() {
        ChunkMetadata copy = new ChunkMetadata();
        if (this.entries != null) {
            copy.entries = new HashMap<>(this.entries);
        }
        return copy;
    }
}
