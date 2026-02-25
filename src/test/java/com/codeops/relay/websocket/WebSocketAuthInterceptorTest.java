package com.codeops.relay.websocket;

import com.codeops.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebSocketAuthInterceptor}.
 *
 * <p>Covers JWT authentication on STOMP CONNECT, Bearer header extraction,
 * native token header fallback, and rejection of invalid/missing tokens.</p>
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private MessageChannel messageChannel;

    @InjectMocks private WebSocketAuthInterceptor interceptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";

    // ── CONNECT with Bearer header ───────────────────────────────────────

    @Nested
    class ConnectWithBearerHeaderTests {

        @Test
        void connect_withValidBearerToken_setsStompPrincipal() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
            accessor.setLeaveMutable(true);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(VALID_TOKEN)).thenReturn(USER_ID);

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertThat(result).isNotNull();
            StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
            assertThat(resultAccessor.getUser()).isInstanceOf(StompPrincipal.class);
            assertThat(resultAccessor.getUser().getName()).isEqualTo(USER_ID.toString());
        }

        @Test
        void connect_withInvalidBearerToken_throwsException() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("Authorization", "Bearer " + INVALID_TOKEN);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            when(jwtTokenProvider.validateToken(INVALID_TOKEN)).thenReturn(false);

            assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("Invalid or missing JWT token");
        }
    }

    // ── CONNECT with native token header ─────────────────────────────────

    @Nested
    class ConnectWithTokenHeaderTests {

        @Test
        void connect_withValidTokenHeader_setsStompPrincipal() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("token", VALID_TOKEN);
            accessor.setLeaveMutable(true);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(VALID_TOKEN)).thenReturn(USER_ID);

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertThat(result).isNotNull();
            StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
            assertThat(resultAccessor.getUser()).isInstanceOf(StompPrincipal.class);
            assertThat(((StompPrincipal) resultAccessor.getUser()).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        void connect_withInvalidTokenHeader_throwsException() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("token", INVALID_TOKEN);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            when(jwtTokenProvider.validateToken(INVALID_TOKEN)).thenReturn(false);

            assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                    .isInstanceOf(MessagingException.class);
        }
    }

    // ── CONNECT without token ────────────────────────────────────────────

    @Nested
    class ConnectWithoutTokenTests {

        @Test
        void connect_noToken_throwsException() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("Invalid or missing JWT token");
        }

        @Test
        void connect_emptyBearerPrefix_throwsException() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("Authorization", "Basic some-creds");
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                    .isInstanceOf(MessagingException.class);
        }
    }

    // ── Non-CONNECT commands ─────────────────────────────────────────────

    @Nested
    class NonConnectCommandTests {

        @Test
        void subscribe_passesThrough() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertThat(result).isNotNull();
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        void send_passesThrough() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertThat(result).isNotNull();
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        void disconnect_passesThrough() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertThat(result).isNotNull();
            verifyNoInteractions(jwtTokenProvider);
        }
    }

    // ── extractToken ─────────────────────────────────────────────────────

    @Nested
    class ExtractTokenTests {

        @Test
        void extractToken_bearerPrefixPriority() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("Authorization", "Bearer bearer-token");
            accessor.addNativeHeader("token", "native-token");

            String token = interceptor.extractToken(accessor);

            assertThat(token).isEqualTo("bearer-token");
        }

        @Test
        void extractToken_fallsBackToNativeToken() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.addNativeHeader("token", "native-token");

            String token = interceptor.extractToken(accessor);

            assertThat(token).isEqualTo("native-token");
        }

        @Test
        void extractToken_noHeaders_returnsNull() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

            String token = interceptor.extractToken(accessor);

            assertThat(token).isNull();
        }
    }
}
