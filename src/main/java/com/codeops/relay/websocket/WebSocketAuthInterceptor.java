package com.codeops.relay.websocket;

import com.codeops.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * STOMP channel interceptor that authenticates WebSocket connections using JWT tokens.
 *
 * <p>On STOMP CONNECT, extracts the JWT token from either the {@code Authorization}
 * header (as {@code "Bearer <token>"}) or a {@code token} native header. Validates
 * the token via {@link JwtTokenProvider} and sets a {@link StompPrincipal} on the
 * session. If validation fails, throws a {@link MessagingException} to reject the
 * connection.</p>
 *
 * <p>Non-CONNECT commands pass through without authentication checks.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Intercepts inbound STOMP messages and authenticates CONNECT commands.
     *
     * @param message the inbound message
     * @param channel the message channel
     * @return the message (unchanged) if authentication succeeds
     * @throws MessagingException if the JWT token is missing or invalid on CONNECT
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                accessor.setUser(new StompPrincipal(userId));
                log.debug("WebSocket CONNECT authenticated for user {}", userId);
            } else {
                log.warn("WebSocket CONNECT rejected: invalid or missing JWT token");
                throw new MessagingException("Invalid or missing JWT token");
            }
        }

        return message;
    }

    /**
     * Extracts the JWT token from STOMP headers.
     *
     * <p>Checks the {@code Authorization} header first (stripping the "Bearer " prefix),
     * then falls back to a {@code token} native header.</p>
     *
     * @param accessor the STOMP header accessor
     * @return the raw JWT string, or null if not found
     */
    String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }
}
