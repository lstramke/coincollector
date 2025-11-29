package io.github.lstramke.coincollector.services;

/**
 * Interface for session management.
 * Defines methods for creating, validating, and managing sessions.
 */
public interface SessionManager {

    /**
     * Creates a new session for a user.
     *
     * @param userId The ID of the user
     * @return The generated session ID
     */
    String createSession(String userId);

    /**
     * Checks if a session is valid.
     *
     * @param sessionId The session ID to validate
     * @return true if the session is valid, false otherwise
     */
    boolean validateSession(String sessionId);

    /**
     * Invalidates a session.
     *
     * @param sessionId The session ID to invalidate
     */
    void invalidateSession(String sessionId);

    /**
     * Returns the user ID associated with a session.
     *
     * @param sessionId The session ID
     * @return The user ID associated with the session
     */
    String getUserId(String sessionId);
}
