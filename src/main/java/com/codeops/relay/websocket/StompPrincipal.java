package com.codeops.relay.websocket;

import java.security.Principal;
import java.util.UUID;

/**
 * Wraps a user ID as a {@link Principal} for STOMP WebSocket sessions.
 *
 * <p>Created during the STOMP CONNECT handshake after JWT validation succeeds.
 * The principal name is the user's UUID string, enabling Spring's user-destination
 * resolution for targeted message delivery (e.g., {@code /user/queue/...}).</p>
 */
public class StompPrincipal implements Principal {

    private final UUID userId;

    /**
     * Creates a new STOMP principal for the given user.
     *
     * @param userId the authenticated user's ID
     */
    public StompPrincipal(UUID userId) {
        this.userId = userId;
    }

    /**
     * Returns the user ID as the principal name.
     *
     * @return the user's UUID as a string
     */
    @Override
    public String getName() {
        return userId.toString();
    }

    /**
     * Returns the user's UUID.
     *
     * @return the user ID
     */
    public UUID getUserId() {
        return userId;
    }
}
