package com.zff.dismantle.storage;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory storage for analysis sessions with automatic cleanup.
 */
@Component
public class ChunkStore {
    private final Map<String, AnalysisSession> sessions = new ConcurrentHashMap<>();
    private final int ttlMinutes;

    // Cleanup expired sessions every 5 minutes
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ChunkStore() {
        this.ttlMinutes = 60;  // Default TTL
        startCleanupTask();
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 5, 5, TimeUnit.MINUTES);
    }

    public AnalysisSession createSession(String sessionId, String originalText) {
        AnalysisSession session = new AnalysisSession(sessionId, originalText, ttlMinutes);
        sessions.put(sessionId, session);
        return session;
    }

    public AnalysisSession getSession(String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session != null && session.isExpired()) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private void cleanupExpired() {
        sessions.values().removeIf(AnalysisSession::isExpired);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
