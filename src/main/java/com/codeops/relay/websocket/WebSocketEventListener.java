package com.codeops.relay.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

/**
 * Listens for WebSocket session lifecycle events (connect/disconnect).
 *
 * <p>On connect, registers the user's session in the {@link WebSocketSessionRegistry}.
 * On disconnect, removes the session. These events drive the WebSocket-level
 * awareness of which users are currently connected.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * Handles a successful STOMP session connection.
     *
     * <p>Extracts the user ID from the {@link StompPrincipal} and the session ID
     * from the STOMP headers, then registers the session in the registry.</p>
     *
     * @param event the session connected event
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (user instanceof StompPrincipal stompPrincipal && sessionId != null) {
            sessionRegistry.registerSession(stompPrincipal.getUserId(), sessionId);
            log.info("WebSocket connected: user={}, session={}", stompPrincipal.getUserId(), sessionId);
        }
    }

    /**
     * Handles a STOMP session disconnect.
     *
     * <p>Removes the session from the registry. If this was the user's last session,
     * they are no longer tracked as online in the WebSocket layer.</p>
     *
     * @param event the session disconnect event
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (user instanceof StompPrincipal stompPrincipal && sessionId != null) {
            sessionRegistry.removeSession(stompPrincipal.getUserId(), sessionId);
            log.info("WebSocket disconnected: user={}, session={}", stompPrincipal.getUserId(), sessionId);
        }
    }
}
