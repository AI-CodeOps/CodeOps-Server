package com.codeops.relay.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the STOMP-over-WebSocket message broker for the Relay module.
 *
 * <p>Registers the {@code /ws/relay} STOMP endpoint with SockJS fallback,
 * configures a simple in-memory message broker for {@code /topic} and
 * {@code /queue} destinations, and wires the {@link WebSocketAuthInterceptor}
 * into the inbound channel for JWT authentication on CONNECT.</p>
 *
 * <p>Destination conventions:</p>
 * <ul>
 *   <li>{@code /app/*} — application-level messages routed to {@code @MessageMapping} methods</li>
 *   <li>{@code /topic/channel.{id}.*} — channel-scoped broadcasts (messages, typing, presence)</li>
 *   <li>{@code /user/queue/*} — user-specific destinations (DMs, notifications)</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * Configures the message broker with simple in-memory destinations.
     *
     * @param config the message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registers the STOMP endpoint at {@code /ws/relay} with SockJS fallback.
     *
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/relay")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Registers the JWT authentication interceptor on the client inbound channel.
     *
     * @param registration the channel registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
