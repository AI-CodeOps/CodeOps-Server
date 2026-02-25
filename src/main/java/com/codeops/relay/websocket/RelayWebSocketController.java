package com.codeops.relay.websocket;

import com.codeops.relay.dto.request.DmTypingRequest;
import com.codeops.relay.dto.request.HeartbeatRequest;
import com.codeops.relay.dto.request.TypingIndicatorRequest;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.DirectConversation;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.DirectConversationRepository;
import com.codeops.relay.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP controller for real-time WebSocket interactions in the Relay module.
 *
 * <p>Handles inbound application-level messages from connected clients:</p>
 * <ul>
 *   <li>{@code /app/channel.typing} — broadcasts a typing indicator to channel members</li>
 *   <li>{@code /app/dm.typing} — sends a typing indicator to DM conversation participants</li>
 *   <li>{@code /app/presence.heartbeat} — processes a presence heartbeat and broadcasts the update</li>
 * </ul>
 *
 * <p>All methods require an authenticated {@link StompPrincipal} established during
 * the STOMP CONNECT handshake.</p>
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class RelayWebSocketController {

    private final RelayWebSocketService webSocketService;
    private final PresenceService presenceService;
    private final ChannelMemberRepository channelMemberRepository;
    private final DirectConversationRepository directConversationRepository;

    /**
     * Handles a typing indicator for a channel.
     *
     * <p>Verifies the sender is a member of the channel before broadcasting.
     * Non-members are silently ignored.</p>
     *
     * @param request   the typing indicator request containing the channel ID
     * @param principal the authenticated STOMP principal
     */
    @MessageMapping("/channel.typing")
    public void handleTypingIndicator(@Payload TypingIndicatorRequest request, Principal principal) {
        UUID userId = getUserId(principal);
        UUID channelId = request.channelId();

        if (channelId == null) {
            log.warn("Typing indicator with null channelId from user {}", userId);
            return;
        }

        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            log.warn("Typing indicator rejected: user {} is not a member of channel {}", userId, channelId);
            return;
        }

        String displayName = webSocketService.resolveDisplayName(userId);
        webSocketService.broadcastTypingIndicator(channelId, userId, displayName);
    }

    /**
     * Handles a typing indicator for a direct message conversation.
     *
     * <p>Verifies the sender is a participant in the conversation before sending
     * the typing indicator to other participants. Non-participants are silently ignored.</p>
     *
     * @param request   the DM typing request containing the conversation ID
     * @param principal the authenticated STOMP principal
     */
    @MessageMapping("/dm.typing")
    public void handleDmTyping(@Payload DmTypingRequest request, Principal principal) {
        UUID userId = getUserId(principal);
        UUID conversationId = request.conversationId();

        if (conversationId == null) {
            log.warn("DM typing indicator with null conversationId from user {}", userId);
            return;
        }

        var conversationOpt = directConversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            log.warn("DM typing rejected: conversation {} not found", conversationId);
            return;
        }

        DirectConversation conversation = conversationOpt.get();
        if (!conversation.getParticipantIds().contains(userId.toString())) {
            log.warn("DM typing rejected: user {} not in conversation {}", userId, conversationId);
            return;
        }

        String displayName = webSocketService.resolveDisplayName(userId);
        String[] participantIds = conversation.getParticipantIds().split(",");
        for (String participantId : participantIds) {
            UUID recipientId = UUID.fromString(participantId.trim());
            if (!recipientId.equals(userId)) {
                webSocketService.sendDmTypingIndicator(recipientId, conversationId, userId, displayName);
            }
        }
    }

    /**
     * Handles a presence heartbeat from a WebSocket client.
     *
     * <p>Delegates to the {@link PresenceService} to record the heartbeat,
     * then broadcasts the updated presence to the team's presence topic.</p>
     *
     * @param request   the heartbeat request containing the team ID
     * @param principal the authenticated STOMP principal
     */
    @MessageMapping("/presence.heartbeat")
    public void handleHeartbeat(@Payload HeartbeatRequest request, Principal principal) {
        UUID userId = getUserId(principal);
        UUID teamId = request.teamId();

        if (teamId == null) {
            log.warn("Heartbeat with null teamId from user {}", userId);
            return;
        }

        UserPresenceResponse presence = presenceService.heartbeat(teamId, userId);
        webSocketService.broadcastPresenceUpdate(teamId, presence);
    }

    /**
     * Extracts the user ID from the STOMP principal.
     *
     * @param principal the authenticated principal
     * @return the user's UUID
     */
    private UUID getUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
