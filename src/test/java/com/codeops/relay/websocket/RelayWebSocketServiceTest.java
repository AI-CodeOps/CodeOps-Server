package com.codeops.relay.websocket;

import com.codeops.entity.User;
import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.dto.response.TypingIndicatorResponse;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RelayWebSocketService}.
 *
 * <p>Covers channel message broadcasting, typing indicators, presence updates,
 * direct message delivery, DM typing indicators, notifications, and display
 * name resolution.</p>
 */
@ExtendWith(MockitoExtension.class)
class RelayWebSocketServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private WebSocketSessionRegistry sessionRegistry;
    @Mock private UserRepository userRepository;

    @InjectMocks private RelayWebSocketService webSocketService;

    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();

    // ── broadcastChannelMessage ──────────────────────────────────────────

    @Nested
    class BroadcastChannelMessageTests {

        @Test
        void broadcastChannelMessage_sendsToCorrectDestination() {
            MessageResponse message = buildMessageResponse();

            webSocketService.broadcastChannelMessage(CHANNEL_ID, message);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/channel." + CHANNEL_ID + ".messages"),
                    eq(message));
        }
    }

    // ── broadcastTypingIndicator ─────────────────────────────────────────

    @Nested
    class BroadcastTypingIndicatorTests {

        @Test
        void broadcastTypingIndicator_sendsToCorrectDestination() {
            webSocketService.broadcastTypingIndicator(CHANNEL_ID, USER_ID, "Test User");

            ArgumentCaptor<TypingIndicatorResponse> captor = ArgumentCaptor.forClass(TypingIndicatorResponse.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/channel." + CHANNEL_ID + ".typing"),
                    captor.capture());

            TypingIndicatorResponse payload = captor.getValue();
            assertThat(payload.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(payload.userId()).isEqualTo(USER_ID);
            assertThat(payload.userDisplayName()).isEqualTo("Test User");
            assertThat(payload.timestamp()).isNotNull();
        }
    }

    // ── broadcastPresenceUpdate ──────────────────────────────────────────

    @Nested
    class BroadcastPresenceUpdateTests {

        @Test
        void broadcastPresenceUpdate_sendsToTeamDestination() {
            UserPresenceResponse presence = new UserPresenceResponse(
                    USER_ID, "Test User", TEAM_ID, PresenceStatus.ONLINE,
                    null, Instant.now(), Instant.now());

            webSocketService.broadcastPresenceUpdate(TEAM_ID, presence);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/team." + TEAM_ID + ".presence"),
                    eq(presence));
        }
    }

    // ── sendDirectMessage ────────────────────────────────────────────────

    @Nested
    class SendDirectMessageTests {

        @Test
        void sendDirectMessage_sendsToUserDestination() {
            DirectMessageResponse dmResponse = buildDirectMessageResponse();

            webSocketService.sendDirectMessage(RECIPIENT_ID, CONVERSATION_ID, dmResponse);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(RECIPIENT_ID.toString()),
                    eq("/queue/dm." + CONVERSATION_ID),
                    eq(dmResponse));
        }
    }

    // ── sendDmTypingIndicator ────────────────────────────────────────────

    @Nested
    class SendDmTypingIndicatorTests {

        @Test
        void sendDmTypingIndicator_sendsToUserQueue() {
            webSocketService.sendDmTypingIndicator(RECIPIENT_ID, CONVERSATION_ID, USER_ID, "Test User");

            ArgumentCaptor<TypingIndicatorResponse> captor = ArgumentCaptor.forClass(TypingIndicatorResponse.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq(RECIPIENT_ID.toString()),
                    eq("/queue/dm.typing"),
                    captor.capture());

            TypingIndicatorResponse payload = captor.getValue();
            assertThat(payload.channelId()).isNull();
            assertThat(payload.userId()).isEqualTo(USER_ID);
            assertThat(payload.userDisplayName()).isEqualTo("Test User");
        }
    }

    // ── sendNotification ─────────────────────────────────────────────────

    @Nested
    class SendNotificationTests {

        @Test
        void sendNotification_sendsToUserQueue() {
            String notification = "You have a new message";

            webSocketService.sendNotification(USER_ID, notification);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(USER_ID.toString()),
                    eq("/queue/notifications"),
                    eq(notification));
        }
    }

    // ── resolveDisplayName ───────────────────────────────────────────────

    @Nested
    class ResolveDisplayNameTests {

        @Test
        void resolveDisplayName_withDisplayName() {
            User user = new User();
            user.setId(USER_ID);
            user.setDisplayName("Test User");
            user.setEmail("test@example.com");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            String result = webSocketService.resolveDisplayName(USER_ID);

            assertThat(result).isEqualTo("Test User");
        }

        @Test
        void resolveDisplayName_withoutDisplayName_fallsBackToEmail() {
            User user = new User();
            user.setId(USER_ID);
            user.setDisplayName(null);
            user.setEmail("test@example.com");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            String result = webSocketService.resolveDisplayName(USER_ID);

            assertThat(result).isEqualTo("test@example.com");
        }

        @Test
        void resolveDisplayName_userNotFound_returnsDefault() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            String result = webSocketService.resolveDisplayName(USER_ID);

            assertThat(result).isEqualTo("Unknown User");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private MessageResponse buildMessageResponse() {
        return new MessageResponse(
                MESSAGE_ID, CHANNEL_ID, USER_ID, "Test User", "Hello world",
                MessageType.TEXT, null, false, null, false, false,
                List.of(), null, List.of(), List.of(), 0, null,
                Instant.now(), Instant.now());
    }

    private DirectMessageResponse buildDirectMessageResponse() {
        return new DirectMessageResponse(
                MESSAGE_ID, CONVERSATION_ID, USER_ID, "Test User", "Hello DM",
                MessageType.TEXT, false, null, false,
                List.of(), List.of(), Instant.now(), Instant.now());
    }
}
