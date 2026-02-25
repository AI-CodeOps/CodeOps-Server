package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.DirectConversationMapper;
import com.codeops.relay.dto.mapper.DirectMessageMapper;
import com.codeops.relay.dto.request.CreateDirectConversationRequest;
import com.codeops.relay.dto.request.SendDirectMessageRequest;
import com.codeops.relay.dto.request.UpdateDirectMessageRequest;
import com.codeops.relay.dto.response.DirectConversationResponse;
import com.codeops.relay.dto.response.DirectConversationSummaryResponse;
import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.entity.DirectConversation;
import com.codeops.relay.entity.DirectMessage;
import com.codeops.relay.entity.ReadReceipt;
import com.codeops.relay.entity.enums.ConversationType;
import com.codeops.relay.repository.DirectConversationRepository;
import com.codeops.relay.repository.DirectMessageRepository;
import com.codeops.relay.repository.ReadReceiptRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for managing direct message conversations between team members.
 *
 * <p>Handles 1-on-1 conversations using {@link DirectConversation} as the container
 * and {@link DirectMessage} for individual messages. Conversations are unique per
 * user-pair within a team, enforced by sorting participant IDs lexicographically
 * before storage. Per-user read tracking uses the {@link ReadReceipt} entity with
 * the conversation ID stored in the channelId field.</p>
 *
 * @see DirectConversation
 * @see DirectMessage
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectConversationRepository directConversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final DirectConversationMapper directConversationMapper;
    private final DirectMessageMapper directMessageMapper;
    private final ReadReceiptRepository readReceiptRepository;

    // ── Conversation Management ───────────────────────────────────────────

    /**
     * Gets or creates a direct conversation between the current user and another user.
     *
     * <p>Participant IDs are sorted lexicographically to ensure the same pair always
     * produces the same participantIds string, making the lookup idempotent. If the
     * conversation already exists, it is returned as-is.</p>
     *
     * @param request the creation request containing the other participant's ID
     * @param teamId  the team ID
     * @param userId  the current user's ID
     * @return the existing or newly created conversation response
     * @throws ValidationException if the user attempts to DM themselves
     * @throws NotFoundException   if either user is not a member of the team
     */
    @Transactional
    public DirectConversationResponse getOrCreateConversation(
            CreateDirectConversationRequest request, UUID teamId, UUID userId) {
        UUID otherUserId = request.participantIds().get(0);

        if (otherUserId.equals(userId)) {
            throw new ValidationException("Cannot create a direct conversation with yourself");
        }

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new NotFoundException("User is not a member of this team");
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, otherUserId)) {
            throw new NotFoundException("Target user is not a member of this team");
        }

        String participantIds = buildParticipantIds(userId, otherUserId);

        return directConversationRepository.findByTeamIdAndParticipantIds(teamId, participantIds)
                .map(existing -> buildConversationResponse(existing))
                .orElseGet(() -> {
                    DirectConversation conversation = DirectConversation.builder()
                            .teamId(teamId)
                            .conversationType(ConversationType.ONE_ON_ONE)
                            .participantIds(participantIds)
                            .build();
                    conversation = directConversationRepository.save(conversation);
                    log.info("Direct conversation created between {} and {} in team {}",
                            userId, otherUserId, teamId);
                    return buildConversationResponse(conversation);
                });
    }

    /**
     * Retrieves a direct conversation by ID with populated metadata.
     *
     * @param conversationId the conversation ID
     * @param userId         the requesting user
     * @return the conversation response
     * @throws NotFoundException      if the conversation does not exist
     * @throws AuthorizationException if the user is not a participant
     */
    @Transactional(readOnly = true)
    public DirectConversationResponse getConversation(UUID conversationId, UUID userId) {
        DirectConversation conversation = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Direct conversation", conversationId));
        verifyParticipant(conversation, userId);
        return buildConversationResponse(conversation);
    }

    /**
     * Retrieves all direct conversations for a user in a team.
     *
     * <p>Uses a LIKE query to find conversations containing the user's ID, then filters
     * in Java to eliminate substring false positives. Results are sorted by most recent
     * message first, with conversations that have no messages sorted last.</p>
     *
     * @param teamId the team ID
     * @param userId the requesting user
     * @return list of conversation summaries sorted by lastMessageAt DESC
     */
    @Transactional(readOnly = true)
    public List<DirectConversationSummaryResponse> getConversations(UUID teamId, UUID userId) {
        List<DirectConversation> candidates = directConversationRepository
                .findByTeamIdAndParticipantIdsContainingOrderByLastMessageAtDesc(
                        teamId, userId.toString());

        return candidates.stream()
                .filter(c -> parseParticipantIds(c.getParticipantIds()).contains(userId))
                .map(c -> buildConversationSummary(c, userId))
                .sorted(Comparator.comparing(
                        DirectConversationSummaryResponse::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Deletes a direct conversation and all its messages.
     *
     * @param conversationId the conversation to delete
     * @param userId         the requesting user
     * @throws NotFoundException      if the conversation does not exist
     * @throws AuthorizationException if the user is not a participant
     */
    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        DirectConversation conversation = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Direct conversation", conversationId));
        verifyParticipant(conversation, userId);

        directMessageRepository.deleteByConversationId(conversationId);
        readReceiptRepository.deleteByChannelId(conversationId);
        directConversationRepository.delete(conversation);

        log.info("Direct conversation {} deleted by {}", conversationId, userId);
    }

    // ── Message Operations ────────────────────────────────────────────────

    /**
     * Sends a message in a direct conversation.
     *
     * <p>Updates the conversation's lastMessageAt and lastMessagePreview
     * after saving the message.</p>
     *
     * @param conversationId the conversation ID
     * @param request        the message content
     * @param senderId       the sending user
     * @return the message response with sender display name
     * @throws NotFoundException      if the conversation does not exist
     * @throws AuthorizationException if the sender is not a participant
     * @throws ValidationException    if the content exceeds the maximum length
     */
    @Transactional
    public DirectMessageResponse sendDirectMessage(UUID conversationId,
                                                    SendDirectMessageRequest request,
                                                    UUID senderId) {
        DirectConversation conversation = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Direct conversation", conversationId));
        verifyParticipant(conversation, senderId);

        if (request.content() != null
                && request.content().length() > AppConstants.RELAY_MAX_MESSAGE_LENGTH) {
            throw new ValidationException("Message content exceeds maximum length of "
                    + AppConstants.RELAY_MAX_MESSAGE_LENGTH + " characters");
        }

        DirectMessage message = directMessageMapper.toEntity(request);
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message = directMessageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.setLastMessagePreview(truncatePreview(request.content()));
        directConversationRepository.save(conversation);

        log.debug("DM sent in conversation {}", conversationId);
        return buildDirectMessageResponse(message);
    }

    /**
     * Retrieves paginated messages in a direct conversation, newest first.
     *
     * @param conversationId the conversation ID
     * @param userId         the requesting user
     * @param page           zero-based page number
     * @param size           page size
     * @return paginated message responses
     * @throws NotFoundException      if the conversation does not exist
     * @throws AuthorizationException if the user is not a participant
     */
    @Transactional(readOnly = true)
    public PageResponse<DirectMessageResponse> getMessages(UUID conversationId, UUID userId,
                                                            int page, int size) {
        DirectConversation conversation = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Direct conversation", conversationId));
        verifyParticipant(conversation, userId);

        Page<DirectMessage> messagePage = directMessageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        conversationId, PageRequest.of(page, size));

        List<DirectMessageResponse> content = messagePage.getContent().stream()
                .map(this::buildDirectMessageResponse)
                .toList();

        return new PageResponse<>(content, page, size,
                messagePage.getTotalElements(), messagePage.getTotalPages(),
                messagePage.isLast());
    }

    /**
     * Edits a previously sent direct message.
     *
     * <p>Only the original sender may edit their message. Sets the isEdited flag
     * and records the edit timestamp.</p>
     *
     * @param messageId the message to edit
     * @param request   the new content
     * @param userId    the requesting user
     * @return the updated message response
     * @throws NotFoundException      if the message does not exist
     * @throws AuthorizationException if the user is not the message sender
     * @throws ValidationException    if the message is deleted or content exceeds max length
     */
    @Transactional
    public DirectMessageResponse editDirectMessage(UUID messageId,
                                                    UpdateDirectMessageRequest request,
                                                    UUID userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Direct message", messageId));

        if (!message.getSenderId().equals(userId)) {
            throw new AuthorizationException("Only the message sender can edit this message");
        }
        if (message.isDeleted()) {
            throw new ValidationException("Cannot edit a deleted message");
        }
        if (request.content() != null
                && request.content().length() > AppConstants.RELAY_MAX_MESSAGE_LENGTH) {
            throw new ValidationException("Message content exceeds maximum length of "
                    + AppConstants.RELAY_MAX_MESSAGE_LENGTH + " characters");
        }

        message.setContent(request.content());
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        message = directMessageRepository.save(message);

        return buildDirectMessageResponse(message);
    }

    /**
     * Soft-deletes a direct message.
     *
     * <p>Only the original sender may delete their own DMs.</p>
     *
     * @param messageId the message to delete
     * @param userId    the requesting user
     * @throws NotFoundException      if the message does not exist
     * @throws AuthorizationException if the user is not the message sender
     */
    @Transactional
    public void deleteDirectMessage(UUID messageId, UUID userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Direct message", messageId));

        if (!message.getSenderId().equals(userId)) {
            throw new AuthorizationException("Only the message sender can delete this message");
        }

        message.setDeleted(true);
        message.setContent("This message was deleted");
        directMessageRepository.save(message);
    }

    // ── Read Tracking ─────────────────────────────────────────────────────

    /**
     * Marks a conversation as read for the given user.
     *
     * <p>Uses the {@link ReadReceipt} entity with the conversation ID stored
     * in the channelId field to track per-user read timestamps.</p>
     *
     * @param conversationId the conversation ID
     * @param userId         the requesting user
     * @throws NotFoundException      if the conversation does not exist
     * @throws AuthorizationException if the user is not a participant
     */
    @Transactional
    public void markConversationRead(UUID conversationId, UUID userId) {
        DirectConversation conversation = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Direct conversation", conversationId));
        verifyParticipant(conversation, userId);

        Instant now = Instant.now();
        ReadReceipt receipt = readReceiptRepository.findByChannelIdAndUserId(conversationId, userId)
                .orElseGet(() -> ReadReceipt.builder()
                        .channelId(conversationId)
                        .userId(userId)
                        .lastReadMessageId(UUID.randomUUID())
                        .build());
        receipt.setLastReadAt(now);
        readReceiptRepository.save(receipt);
    }

    /**
     * Returns the number of unread messages from the other participant.
     *
     * <p>Counts non-deleted messages sent by other users after the user's
     * last read timestamp. If no read receipt exists, all messages from
     * other participants are counted.</p>
     *
     * @param conversationId the conversation ID
     * @param userId         the requesting user
     * @return the unread message count
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID conversationId, UUID userId) {
        return readReceiptRepository.findByChannelIdAndUserId(conversationId, userId)
                .map(receipt -> directMessageRepository
                        .countByConversationIdAndSenderIdNotAndCreatedAtAfterAndIsDeletedFalse(
                                conversationId, userId, receipt.getLastReadAt()))
                .orElseGet(() -> directMessageRepository
                        .countByConversationIdAndSenderIdNotAndIsDeletedFalse(
                                conversationId, userId));
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    /**
     * Verifies that a user is a participant in the conversation.
     *
     * @param conversation the conversation entity
     * @param userId       the user to verify
     * @throws AuthorizationException if the user is not a participant
     */
    private void verifyParticipant(DirectConversation conversation, UUID userId) {
        List<UUID> participants = parseParticipantIds(conversation.getParticipantIds());
        if (!participants.contains(userId)) {
            throw new AuthorizationException("User is not a participant in this conversation");
        }
    }

    /**
     * Returns the other participant's ID in a 1:1 conversation.
     *
     * @param conversation the conversation entity
     * @param userId       the current user's ID
     * @return the other participant's UUID
     */
    private UUID getOtherParticipantId(DirectConversation conversation, UUID userId) {
        return parseParticipantIds(conversation.getParticipantIds()).stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(userId);
    }

    /**
     * Resolves a user's display name, falling back to email or "Unknown User".
     *
     * @param userId the user ID
     * @return the display name
     */
    private String resolveDisplayName(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getDisplayName() != null ? user.getDisplayName() : user.getEmail())
                .orElse("Unknown User");
    }

    /**
     * Builds a sorted, comma-separated participant IDs string.
     *
     * <p>Sorting ensures the same pair always produces the same string,
     * enabling idempotent conversation lookup.</p>
     *
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return sorted comma-separated string
     */
    static String buildParticipantIds(UUID userId1, UUID userId2) {
        return Stream.of(userId1.toString(), userId2.toString())
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * Parses a comma-separated participant IDs string into a list of UUIDs.
     *
     * @param participantIds the comma-separated string
     * @return list of participant UUIDs
     */
    private List<UUID> parseParticipantIds(String participantIds) {
        if (participantIds == null || participantIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(participantIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
    }

    /**
     * Truncates content for the lastMessagePreview field (max 100 chars).
     *
     * @param content the full message content
     * @return truncated preview
     */
    private String truncatePreview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= 100 ? content : content.substring(0, 100) + "...";
    }

    /**
     * Builds a DirectConversationResponse with parsed participant IDs.
     *
     * @param conversation the conversation entity
     * @return the response DTO
     */
    private DirectConversationResponse buildConversationResponse(DirectConversation conversation) {
        List<UUID> participantIds = parseParticipantIds(conversation.getParticipantIds());
        return new DirectConversationResponse(
                conversation.getId(), conversation.getTeamId(),
                conversation.getConversationType(), conversation.getName(),
                participantIds, conversation.getLastMessageAt(),
                conversation.getLastMessagePreview(),
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }

    /**
     * Builds a DirectConversationSummaryResponse with display names and unread count.
     *
     * @param conversation the conversation entity
     * @param userId       the current user (for unread calculation)
     * @return the summary response DTO
     */
    private DirectConversationSummaryResponse buildConversationSummary(
            DirectConversation conversation, UUID userId) {
        List<UUID> participantIds = parseParticipantIds(conversation.getParticipantIds());
        List<String> displayNames = participantIds.stream()
                .map(this::resolveDisplayName)
                .toList();
        long unreadCount = getUnreadCount(conversation.getId(), userId);

        return new DirectConversationSummaryResponse(
                conversation.getId(), conversation.getConversationType(),
                conversation.getName(), participantIds, displayNames,
                conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(), unreadCount);
    }

    /**
     * Builds a DirectMessageResponse with sender display name.
     *
     * <p>Reactions and attachments are returned as empty lists — population
     * will be added when the ReactionService and FileService are implemented.</p>
     *
     * @param message the direct message entity
     * @return the response DTO
     */
    private DirectMessageResponse buildDirectMessageResponse(DirectMessage message) {
        String senderDisplayName = resolveDisplayName(message.getSenderId());
        return new DirectMessageResponse(
                message.getId(), message.getConversationId(), message.getSenderId(),
                senderDisplayName, message.getContent(), message.getMessageType(),
                message.isEdited(), message.getEditedAt(), message.isDeleted(),
                List.of(), List.of(),
                message.getCreatedAt(), message.getUpdatedAt());
    }
}
