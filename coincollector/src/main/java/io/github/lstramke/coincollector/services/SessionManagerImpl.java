package io.github.lstramke.coincollector.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * Concrete implementation of the {@link SessionManager} interface.
 * <p>
 * This class manages user sessions in-memory using a {@link HashMap}.
 * Each session is identified by a UUID and mapped to a user ID.
 * Sessions are created, validated, invalidated, and queried via the methods defined in the interface.
 * <p>
 * <b>Note:</b> This implementation is not persistent and is suitable only for single-instance applications.
 */
public class SessionManagerImpl implements SessionManager {
    private final Map<String, String> sessions = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, userId);
        return sessionId;
    }

    /**
     * {@inheritDoc}
     */
    public boolean validateSession(String sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    /**
     * {@inheritDoc}
     */
    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * {@inheritDoc}
     */
    public String getUserId(String sessionId) {
        return sessions.get(sessionId);
    }
}
