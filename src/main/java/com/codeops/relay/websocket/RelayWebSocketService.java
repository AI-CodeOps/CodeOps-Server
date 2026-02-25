package com.codeops.relay.websocket;

import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.dto.response.TypingIndicatorResponse;
import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for broadcasting real-time events to WebSocket subscribers.
 *
 * <p>Provides methods for pushing channel messages, typing indicators, presence
 * updates, direct messages, and user-targeted notifications through the STOMP
 * message broker. All broadcasts are fire-and-forget — failures are logged but
 * do not propagate exceptions to callers.</p>
 *
 * <p>Destination conventions:</p>
 * <ul>
 *   <li>{@code /topic/channel.{channelId}.messages} — channel message broadcasts</li>
 *   <li>{@code /topic/channel.{channelId}.typing} — channel typing indicators</li>
 *   <li>{@code /topic/team.{teamId}.presence} — team-wide presence updates</li>
 *   <li>{@code /queue/dm.{conversationId}} — user-targeted DM delivery</li>
 *   <li>{@code /queue/dm.typing} — user-targeted DM typing indicators</li>
 *   <li>{@code /queue/notifications} — user-targeted notifications</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RelayWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;
    private final UserRepository userRepository;

    /**
     * Broadcasts a new or updated message to all subscribers of a channel.
     *
     * @param channelId the channel ID
     * @param message   the message response payload
     */
    public void broadcastChannelMessage(UUID channelId, MessageResponse message) {
        String destination = "/topic/channel." + channelId + ".messages";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcast message to {}: messageId={}", destination, message.id());
    }

    /**
     * Broadcasts a typing indicator to all subscribers of a channel.
     *
     * @param channelId   the channel ID
     * @param userId      the user who is typing
     * @param displayName the display name of the typing user
     */
    public void broadcastTypingIndicator(UUID channelId, UUID userId, String displayName) {
        String destination = "/topic/channel." + channelId + ".typing";
        TypingIndicatorResponse payload = new TypingIndicatorResponse(
                channelId, userId, displayName, Instant.now());
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Broadcast typing to {}: user={}", destination, userId);
    }

    /**
     * Broadcasts a presence update to all subscribers of a team's presence topic.
     *
     * @param teamId   the team ID
     * @param presence the updated presence response
     */
    public void broadcastPresenceUpdate(UUID teamId, UserPresenceResponse presence) {
        String destination = "/topic/team." + teamId + ".presence";
        messagingTemplate.convertAndSend(destination, presence);
        log.debug("Broadcast presence to {}: user={}, status={}",
                destination, presence.userId(), presence.status());
    }

    /**
     * Sends a direct message to a specific user via their user-destination queue.
     *
     * @param recipientId    the recipient's user ID
     * @param conversationId the conversation ID
     * @param message        the direct message response payload
     */
    public void sendDirectMessage(UUID recipientId, UUID conversationId,
                                  DirectMessageResponse message) {
        String destination = "/queue/dm." + conversationId;
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(), destination, message);
        log.debug("Sent DM to user {}: conversation={}, messageId={}",
                recipientId, conversationId, message.id());
    }

    /**
     * Sends a DM typing indicator to a specific user.
     *
     * @param recipientId    the recipient's user ID
     * @param conversationId the conversation ID
     * @param userId         the user who is typing
     * @param displayName    the display name of the typing user
     */
    public void sendDmTypingIndicator(UUID recipientId, UUID conversationId,
                                      UUID userId, String displayName) {
        TypingIndicatorResponse payload = new TypingIndicatorResponse(
                null, userId, displayName, Instant.now());
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(), "/queue/dm.typing", payload);
        log.debug("Sent DM typing to user {}: conversation={}, typingUser={}",
                recipientId, conversationId, userId);
    }

    /**
     * Sends a notification payload to a specific user's notification queue.
     *
     * @param userId       the recipient's user ID
     * @param notification the notification payload
     */
    public void sendNotification(UUID userId, Object notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(), "/queue/notifications", notification);
        log.debug("Sent notification to user {}", userId);
    }

    /**
     * Resolves a display name for a user, falling back to email.
     *
     * @param userId the user ID
     * @return the display name or email, or "Unknown User" if not found
     */
    String resolveDisplayName(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getDisplayName() != null ? user.getDisplayName() : user.getEmail())
                .orElse("Unknown User");
    }
}
