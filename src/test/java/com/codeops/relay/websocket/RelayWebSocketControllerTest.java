package com.codeops.relay.websocket;

import com.codeops.relay.dto.request.DmTypingRequest;
import com.codeops.relay.dto.request.HeartbeatRequest;
import com.codeops.relay.dto.request.TypingIndicatorRequest;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.DirectConversation;
import com.codeops.relay.entity.enums.ConversationType;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.DirectConversationRepository;
import com.codeops.relay.service.PresenceService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RelayWebSocketController}.
 *
 * <p>Covers channel typing indicators, DM typing indicators, and presence
 * heartbeats, including authorization checks and edge cases.</p>
 */
@ExtendWith(MockitoExtension.class)
class RelayWebSocketControllerTest {

    @Mock private RelayWebSocketService webSocketService;
    @Mock private PresenceService presenceService;
    @Mock private ChannelMemberRepository channelMemberRepository;
    @Mock private DirectConversationRepository directConversationRepository;

    @InjectMocks private RelayWebSocketController controller;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_2_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();

    private final StompPrincipal principal = new StompPrincipal(USER_ID);

    // ── handleTypingIndicator ────────────────────────────────────────────

    @Nested
    class HandleTypingIndicatorTests {

        @Test
        void handleTypingIndicator_memberOfChannel_broadcasts() {
            var request = new TypingIndicatorRequest(CHANNEL_ID);
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(webSocketService.resolveDisplayName(USER_ID)).thenReturn("Test User");

            controller.handleTypingIndicator(request, principal);

            verify(webSocketService).broadcastTypingIndicator(CHANNEL_ID, USER_ID, "Test User");
        }

        @Test
        void handleTypingIndicator_notMember_doesNotBroadcast() {
            var request = new TypingIndicatorRequest(CHANNEL_ID);
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(false);

            controller.handleTypingIndicator(request, principal);

            verify(webSocketService, never()).broadcastTypingIndicator(any(), any(), any());
        }

        @Test
        void handleTypingIndicator_nullChannelId_doesNotBroadcast() {
            var request = new TypingIndicatorRequest(null);

            controller.handleTypingIndicator(request, principal);

            verify(webSocketService, never()).broadcastTypingIndicator(any(), any(), any());
            verifyNoInteractions(channelMemberRepository);
        }
    }

    // ── handleDmTyping ───────────────────────────────────────────────────

    @Nested
    class HandleDmTypingTests {

        @Test
        void handleDmTyping_validParticipant_sendsToOtherParticipants() {
            var request = new DmTypingRequest(CONVERSATION_ID);
            String participantIds = sortParticipants(USER_ID, USER_2_ID);
            DirectConversation conversation = buildConversation(participantIds);

            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));
            when(webSocketService.resolveDisplayName(USER_ID)).thenReturn("Test User");

            controller.handleDmTyping(request, principal);

            verify(webSocketService).sendDmTypingIndicator(USER_2_ID, CONVERSATION_ID, USER_ID, "Test User");
            verify(webSocketService, never()).sendDmTypingIndicator(
                    eq(USER_ID), any(), any(), any());
        }

        @Test
        void handleDmTyping_conversationNotFound_doesNotSend() {
            var request = new DmTypingRequest(CONVERSATION_ID);
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.empty());

            controller.handleDmTyping(request, principal);

            verify(webSocketService, never()).sendDmTypingIndicator(any(), any(), any(), any());
        }

        @Test
        void handleDmTyping_userNotParticipant_doesNotSend() {
            var request = new DmTypingRequest(CONVERSATION_ID);
            UUID otherUser = UUID.randomUUID();
            String participantIds = sortParticipants(USER_2_ID, otherUser);
            DirectConversation conversation = buildConversation(participantIds);

            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            controller.handleDmTyping(request, principal);

            verify(webSocketService, never()).sendDmTypingIndicator(any(), any(), any(), any());
        }

        @Test
        void handleDmTyping_nullConversationId_doesNotSend() {
            var request = new DmTypingRequest(null);

            controller.handleDmTyping(request, principal);

            verify(webSocketService, never()).sendDmTypingIndicator(any(), any(), any(), any());
            verifyNoInteractions(directConversationRepository);
        }
    }

    // ── handleHeartbeat ──────────────────────────────────────────────────

    @Nested
    class HandleHeartbeatTests {

        @Test
        void handleHeartbeat_delegatesToPresenceService() {
            var request = new HeartbeatRequest(TEAM_ID);
            UserPresenceResponse presence = new UserPresenceResponse(
                    USER_ID, "Test User", TEAM_ID, PresenceStatus.ONLINE,
                    null, Instant.now(), Instant.now());
            when(presenceService.heartbeat(TEAM_ID, USER_ID)).thenReturn(presence);

            controller.handleHeartbeat(request, principal);

            verify(presenceService).heartbeat(TEAM_ID, USER_ID);
            verify(webSocketService).broadcastPresenceUpdate(TEAM_ID, presence);
        }

        @Test
        void handleHeartbeat_nullTeamId_doesNotProcess() {
            var request = new HeartbeatRequest(null);

            controller.handleHeartbeat(request, principal);

            verifyNoInteractions(presenceService);
            verify(webSocketService, never()).broadcastPresenceUpdate(any(), any());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private DirectConversation buildConversation(String participantIds) {
        DirectConversation conversation = new DirectConversation();
        conversation.setId(CONVERSATION_ID);
        conversation.setTeamId(TEAM_ID);
        conversation.setConversationType(ConversationType.ONE_ON_ONE);
        conversation.setParticipantIds(participantIds);
        return conversation;
    }

    private String sortParticipants(UUID user1, UUID user2) {
        String s1 = user1.toString();
        String s2 = user2.toString();
        return s1.compareTo(s2) < 0 ? s1 + "," + s2 : s2 + "," + s1;
    }
}
