package com.codeops.relay.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry that tracks active WebSocket sessions by user ID.
 *
 * <p>Maintains a mapping from user UUIDs to their active STOMP session IDs.
 * A single user may have multiple concurrent sessions (e.g., multiple browser
 * tabs). Used by {@link WebSocketEventListener} to register/remove sessions
 * and by {@link RelayWebSocketService} to check online status.</p>
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * Registers a WebSocket session for a user.
     *
     * @param userId    the user's ID
     * @param sessionId the STOMP session ID
     */
    public void registerSession(UUID userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        log.debug("Session registered: user={}, session={}, total={}",
                userId, sessionId, userSessions.get(userId).size());
    }

    /**
     * Removes a WebSocket session for a user.
     *
     * <p>If the user has no remaining sessions, their entry is removed from the registry.</p>
     *
     * @param userId    the user's ID
     * @param sessionId the STOMP session ID to remove
     */
    public void removeSession(UUID userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
            log.debug("Session removed: user={}, session={}, remaining={}",
                    userId, sessionId, sessions.size());
        }
    }

    /**
     * Returns all active session IDs for a user.
     *
     * @param userId the user's ID
     * @return unmodifiable set of session IDs, or empty set if none
     */
    public Set<String> getSessionIds(UUID userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? Collections.unmodifiableSet(sessions) : Collections.emptySet();
    }

    /**
     * Checks whether a user has at least one active WebSocket session.
     *
     * @param userId the user's ID
     * @return true if the user has one or more active sessions
     */
    public boolean isUserOnline(UUID userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Returns all user IDs with active WebSocket sessions.
     *
     * @return unmodifiable set of online user IDs
     */
    public Set<UUID> getOnlineUserIds() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    /**
     * Returns the total number of active WebSocket sessions across all users.
     *
     * @return total session count
     */
    public int getActiveSessionCount() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}
