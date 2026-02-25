package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.TeamRole;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.MessageMapper;
import com.codeops.relay.dto.request.MarkReadRequest;
import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.dto.request.UpdateMessageRequest;
import com.codeops.relay.dto.response.ChannelSearchResultResponse;
import com.codeops.relay.dto.response.FileAttachmentResponse;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.dto.response.ReadReceiptResponse;
import com.codeops.relay.dto.response.ReactionSummaryResponse;
import com.codeops.relay.dto.response.UnreadCountResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.ChannelMember;
import com.codeops.relay.entity.FileAttachment;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.MessageThread;
import com.codeops.relay.entity.Reaction;
import com.codeops.relay.entity.ReadReceipt;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.repository.FileAttachmentRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.ReactionRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core messaging service for Relay channels.
 *
 * <p>Handles sending, editing, and soft-deleting messages, threaded replies,
 * @mentions, message search (single-channel and cross-channel), unread tracking,
 * and read receipts. Builds fully populated {@link MessageResponse} objects with
 * sender display names, reaction summaries, file attachments, and thread metadata.</p>
 *
 * @see Message
 * @see ThreadService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ReadReceiptRepository readReceiptRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final ThreadService threadService;
    private final ReactionRepository reactionRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final TeamMemberRepository teamMemberRepository;

    // ── Sending ───────────────────────────────────────────────────────────

    /**
     * Sends a message in a channel.
     *
     * <p>Validates channel membership and archive status, serializes @mention user IDs,
     * delegates thread tracking to {@link ThreadService} if this is a reply, and
     * auto-marks the sender's read receipt to this message.</p>
     *
     * @param channelId the target channel ID
     * @param request   the message content and optional metadata
     * @param teamId    the team owning the channel
     * @param senderId  the user sending the message
     * @return the fully populated message response
     * @throws NotFoundException      if the channel or parent message does not exist
     * @throws AuthorizationException if the sender is not a channel member
     * @throws ValidationException    if the channel is archived or content exceeds max length
     */
    @Transactional
    public MessageResponse sendMessage(UUID channelId, SendMessageRequest request,
                                       UUID teamId, UUID senderId) {
        Channel channel = findChannelByIdAndTeam(channelId, teamId);
        ChannelMember membership = verifyChannelMember(channelId, senderId);

        if (channel.isArchived()) {
            throw new ValidationException("Cannot send messages in an archived channel");
        }

        if (request.content() != null
                && request.content().length() > AppConstants.RELAY_MAX_MESSAGE_LENGTH) {
            throw new ValidationException("Message content exceeds maximum length of "
                    + AppConstants.RELAY_MAX_MESSAGE_LENGTH + " characters");
        }

        Message message = messageMapper.toEntity(request);
        message.setChannelId(channelId);
        message.setSenderId(senderId);
        message.setMessageType(MessageType.TEXT);

        if (request.mentionedUserIds() != null && !request.mentionedUserIds().isEmpty()) {
            message.setMentionedUserIds(request.mentionedUserIds().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(",")));
        }
        message.setMentionsEveryone(
                request.mentionsEveryone() != null && request.mentionsEveryone());

        if (request.parentId() != null) {
            Message parentMessage = messageRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("Parent message", request.parentId()));
            if (!parentMessage.getChannelId().equals(channelId)) {
                throw new ValidationException(
                        "Parent message does not belong to this channel");
            }
            message.setParentId(request.parentId());
            threadService.onReply(request.parentId(), channelId, senderId);
        }

        message = messageRepository.save(message);

        upsertReadReceipt(channelId, senderId, message.getId(), membership);

        log.info("Message sent in channel {} by {}", channelId, senderId);
        return populateMessageResponse(message, senderId);
    }

    // ── Getting ───────────────────────────────────────────────────────────

    /**
     * Retrieves a single message by ID with all populated fields.
     *
     * @param messageId the message ID
     * @return the fully populated message response
     * @throws NotFoundException if the message does not exist
     */
    @Transactional(readOnly = true)
    public MessageResponse getMessage(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message", messageId));
        return populateMessageResponse(message, null);
    }

    /**
     * Retrieves paginated top-level messages in a channel, newest first.
     *
     * <p>Only returns non-deleted messages with no parent (thread root messages
     * and standalone messages). Each message is fully populated with sender name,
     * reactions, attachments, and thread reply counts.</p>
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @param page      zero-based page number
     * @param size      page size
     * @return paginated message responses
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user is not a channel member
     */
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getChannelMessages(UUID channelId, UUID teamId,
                                                             UUID userId, int page, int size) {
        findChannelByIdAndTeam(channelId, teamId);
        verifyChannelMember(channelId, userId);

        Page<Message> messagePage = messageRepository
                .findByChannelIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelId, PageRequest.of(page, size));

        List<MessageResponse> content = messagePage.getContent().stream()
                .map(m -> populateMessageResponse(m, userId))
                .toList();

        return new PageResponse<>(content, page, size,
                messagePage.getTotalElements(), messagePage.getTotalPages(),
                messagePage.isLast());
    }

    /**
     * Retrieves all non-deleted replies to a parent message, ordered chronologically.
     *
     * @param parentMessageId the parent (root) message ID
     * @param userId          the requesting user
     * @return list of reply responses ordered by createdAt ASC
     * @throws NotFoundException      if the parent message does not exist
     * @throws AuthorizationException if the user is not a member of the parent's channel
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getThreadReplies(UUID parentMessageId, UUID userId) {
        Message parentMessage = messageRepository.findById(parentMessageId)
                .orElseThrow(() -> new NotFoundException("Message", parentMessageId));
        verifyChannelMember(parentMessage.getChannelId(), userId);

        List<Message> replies = messageRepository
                .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentMessageId);

        return replies.stream()
                .map(m -> populateMessageResponse(m, userId))
                .toList();
    }

    // ── Editing ───────────────────────────────────────────────────────────

    /**
     * Edits a previously sent message.
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
    public MessageResponse editMessage(UUID messageId, UpdateMessageRequest request, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message", messageId));

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
        message = messageRepository.save(message);

        return populateMessageResponse(message, userId);
    }

    // ── Deleting ──────────────────────────────────────────────────────────

    /**
     * Soft-deletes a message by replacing its content.
     *
     * <p>The sender can delete their own message. Channel OWNERs/ADMINs and team
     * ADMIN/OWNER users can delete any message in the channel. Reactions and
     * attachments are not removed (they become orphaned but invisible).</p>
     *
     * @param messageId the message to delete
     * @param userId    the requesting user
     * @param teamId    the team ID (for team-level auth fallback)
     * @throws NotFoundException      if the message does not exist
     * @throws AuthorizationException if the user lacks permission to delete
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId, UUID teamId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message", messageId));

        if (!message.getSenderId().equals(userId)) {
            verifyDeletePermission(message.getChannelId(), teamId, userId);
        }

        message.setDeleted(true);
        message.setContent("This message was deleted");
        messageRepository.save(message);

        log.info("Message {} deleted by {}", messageId, userId);
    }

    // ── Search ────────────────────────────────────────────────────────────

    /**
     * Searches messages within a single channel by content substring.
     *
     * @param channelId the channel to search
     * @param query     the search term
     * @param teamId    the team ID
     * @param userId    the requesting user
     * @param page      zero-based page number
     * @param size      page size
     * @return paginated search results
     * @throws NotFoundException      if the channel does not exist
     * @throws AuthorizationException if the user is not a channel member
     * @throws ValidationException    if the query is blank
     */
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> searchMessages(UUID channelId, String query,
                                                         UUID teamId, UUID userId,
                                                         int page, int size) {
        findChannelByIdAndTeam(channelId, teamId);
        verifyChannelMember(channelId, userId);

        if (query == null || query.isBlank()) {
            throw new ValidationException("Search query cannot be blank");
        }

        Page<Message> results = messageRepository.searchInChannel(
                channelId, query, PageRequest.of(page, size));

        List<MessageResponse> content = results.getContent().stream()
                .map(m -> populateMessageResponse(m, userId))
                .toList();

        return new PageResponse<>(content, page, size,
                results.getTotalElements(), results.getTotalPages(),
                results.isLast());
    }

    /**
     * Searches messages across all channels the user belongs to.
     *
     * <p>Builds {@link ChannelSearchResultResponse} with channel name, sender display name,
     * and a content snippet (~100 characters around the match).</p>
     *
     * @param query  the search term
     * @param teamId the team ID
     * @param userId the requesting user
     * @param page   zero-based page number
     * @param size   page size
     * @return paginated cross-channel search results
     * @throws ValidationException if the query is blank
     */
    @Transactional(readOnly = true)
    public PageResponse<ChannelSearchResultResponse> searchMessagesAcrossChannels(
            String query, UUID teamId, UUID userId, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Search query cannot be blank");
        }

        List<UUID> channelIds = channelMemberRepository.findChannelIdsByUserId(userId);
        if (channelIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0, true);
        }

        Page<Message> results = messageRepository.searchAcrossChannels(
                channelIds, query, PageRequest.of(page, size));

        Map<UUID, String> channelNameCache = new LinkedHashMap<>();
        List<ChannelSearchResultResponse> content = results.getContent().stream()
                .map(m -> {
                    String channelName = channelNameCache.computeIfAbsent(
                            m.getChannelId(),
                            id -> channelRepository.findById(id)
                                    .map(Channel::getName)
                                    .orElse("Unknown Channel"));
                    String senderDisplayName = resolveDisplayName(m.getSenderId());
                    String snippet = buildContentSnippet(m.getContent(), query);
                    return new ChannelSearchResultResponse(
                            m.getId(), m.getChannelId(), channelName,
                            m.getSenderId(), senderDisplayName,
                            snippet, m.getCreatedAt());
                })
                .toList();

        return new PageResponse<>(content, page, size,
                results.getTotalElements(), results.getTotalPages(),
                results.isLast());
    }

    // ── Read Receipts ─────────────────────────────────────────────────────

    /**
     * Marks messages as read up to a given message ID in a channel.
     *
     * <p>Creates or updates the user's read receipt and also updates the
     * channel member's lastReadAt timestamp.</p>
     *
     * @param channelId the channel ID
     * @param request   the mark-read request containing the last read message ID
     * @param userId    the requesting user
     * @return the read receipt response
     * @throws AuthorizationException if the user is not a channel member
     */
    @Transactional
    public ReadReceiptResponse markRead(UUID channelId, MarkReadRequest request, UUID userId) {
        ChannelMember membership = verifyChannelMember(channelId, userId);

        Instant now = Instant.now();
        ReadReceipt receipt = readReceiptRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseGet(() -> ReadReceipt.builder()
                        .channelId(channelId)
                        .userId(userId)
                        .build());

        receipt.setLastReadMessageId(request.lastReadMessageId());
        receipt.setLastReadAt(now);
        readReceiptRepository.save(receipt);

        membership.setLastReadAt(now);
        channelMemberRepository.save(membership);

        return new ReadReceiptResponse(channelId, userId,
                request.lastReadMessageId(), now);
    }

    /**
     * Returns unread message counts for all channels the user belongs to in a team.
     *
     * <p>Only channels with unread messages are included. Results are sorted
     * by unread count descending.</p>
     *
     * @param teamId the team ID
     * @param userId the requesting user
     * @return list of unread count responses, sorted by unreadCount DESC
     */
    @Transactional(readOnly = true)
    public List<UnreadCountResponse> getUnreadCounts(UUID teamId, UUID userId) {
        List<UUID> channelIds = channelMemberRepository.findChannelIdsByUserId(userId);
        List<UnreadCountResponse> results = new ArrayList<>();

        for (UUID channelId : channelIds) {
            Channel channel = channelRepository.findById(channelId).orElse(null);
            if (channel == null || !channel.getTeamId().equals(teamId)) {
                continue;
            }

            ChannelMember membership = channelMemberRepository
                    .findByChannelIdAndUserId(channelId, userId).orElse(null);
            if (membership == null) {
                continue;
            }

            Instant lastReadAt = membership.getLastReadAt();
            long unreadCount = messageRepository.countUnreadMessages(
                    channelId, lastReadAt != null ? lastReadAt : Instant.EPOCH);

            if (unreadCount > 0) {
                results.add(new UnreadCountResponse(
                        channelId, channel.getName(), channel.getSlug(),
                        unreadCount, lastReadAt));
            }
        }

        results.sort((a, b) -> Long.compare(b.unreadCount(), a.unreadCount()));
        return results;
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    /**
     * Assembles a fully populated MessageResponse with display name, reactions,
     * attachments, and thread info.
     *
     * @param message       the message entity
     * @param currentUserId the current user (for reaction currentUserReacted), may be null
     * @return fully populated response
     */
    private MessageResponse populateMessageResponse(Message message, UUID currentUserId) {
        String senderDisplayName = resolveDisplayName(message.getSenderId());
        List<ReactionSummaryResponse> reactions = buildReactionSummaries(
                message.getId(), currentUserId);
        List<FileAttachmentResponse> attachments = buildAttachmentResponses(message.getId());
        List<UUID> mentionedIds = parseMentionedUserIds(message.getMentionedUserIds());

        int replyCount = 0;
        Instant lastReplyAt = null;
        Optional<MessageThread> threadInfo = threadService.getThreadInfo(message.getId());
        if (threadInfo.isPresent()) {
            replyCount = threadInfo.get().getReplyCount();
            lastReplyAt = threadInfo.get().getLastReplyAt();
        }

        return new MessageResponse(
                message.getId(), message.getChannelId(), message.getSenderId(),
                senderDisplayName, message.getContent(), message.getMessageType(),
                message.getParentId(), message.isEdited(), message.getEditedAt(),
                message.isDeleted(), message.isMentionsEveryone(), mentionedIds,
                message.getPlatformEventId(), reactions, attachments,
                replyCount, lastReplyAt,
                message.getCreatedAt(), message.getUpdatedAt());
    }

    /**
     * Builds aggregated reaction summaries grouped by emoji.
     *
     * @param messageId     the message ID
     * @param currentUserId the current user (for currentUserReacted flag), may be null
     * @return list of reaction summaries sorted by count DESC
     */
    private List<ReactionSummaryResponse> buildReactionSummaries(UUID messageId,
                                                                  UUID currentUserId) {
        List<Reaction> reactions = reactionRepository.findByMessageId(messageId);
        if (reactions.isEmpty()) {
            return List.of();
        }

        Map<String, List<Reaction>> byEmoji = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji, LinkedHashMap::new,
                        Collectors.toList()));

        return byEmoji.entrySet().stream()
                .map(entry -> {
                    String emoji = entry.getKey();
                    List<Reaction> group = entry.getValue();
                    List<UUID> userIds = group.stream()
                            .map(Reaction::getUserId)
                            .toList();
                    boolean currentUserReacted = currentUserId != null
                            && userIds.contains(currentUserId);
                    return new ReactionSummaryResponse(
                            emoji, group.size(), currentUserReacted, userIds);
                })
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();
    }

    /**
     * Builds file attachment responses for a message.
     *
     * @param messageId the message ID
     * @return list of attachment responses
     */
    private List<FileAttachmentResponse> buildAttachmentResponses(UUID messageId) {
        List<FileAttachment> attachments = fileAttachmentRepository.findByMessageId(messageId);
        return attachments.stream()
                .map(a -> new FileAttachmentResponse(
                        a.getId(), a.getFileName(), a.getContentType(),
                        a.getFileSizeBytes(), null, null,
                        a.getStatus(), a.getUploadedBy(), a.getCreatedAt()))
                .toList();
    }

    /**
     * Extracts a content snippet of ~100 characters centered around the query match.
     *
     * @param content the full message content
     * @param query   the search term
     * @return the content snippet
     */
    private String buildContentSnippet(String content, String query) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        int idx = content.toLowerCase().indexOf(query.toLowerCase());
        if (idx < 0) {
            return content.length() <= 100 ? content : content.substring(0, 100) + "...";
        }

        int start = Math.max(0, idx - 50);
        int end = Math.min(content.length(), idx + query.length() + 50);
        String snippet = content.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

    /**
     * Parses comma-separated mentioned user IDs into a list of UUIDs.
     *
     * @param mentionedUserIds the comma-separated string
     * @return list of UUIDs, empty if null or blank
     */
    private List<UUID> parseMentionedUserIds(String mentionedUserIds) {
        if (mentionedUserIds == null || mentionedUserIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(mentionedUserIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
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
     * Finds a channel by ID and verifies it belongs to the expected team.
     *
     * @param channelId the channel ID
     * @param teamId    the expected team ID
     * @return the channel entity
     * @throws NotFoundException if the channel does not exist or belongs to a different team
     */
    private Channel findChannelByIdAndTeam(UUID channelId, UUID teamId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel", channelId));
        if (!channel.getTeamId().equals(teamId)) {
            throw new NotFoundException("Channel", channelId);
        }
        return channel;
    }

    /**
     * Verifies that a user is a member of the specified channel.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     * @return the channel membership
     * @throws AuthorizationException if the user is not a channel member
     */
    private ChannelMember verifyChannelMember(UUID channelId, UUID userId) {
        return channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new AuthorizationException(
                        "User is not a member of this channel"));
    }

    /**
     * Verifies delete permission: channel OWNER/ADMIN or team ADMIN/OWNER.
     *
     * @param channelId the channel ID
     * @param teamId    the team ID
     * @param userId    the user attempting to delete
     * @throws AuthorizationException if the user lacks permission
     */
    private void verifyDeletePermission(UUID channelId, UUID teamId, UUID userId) {
        ChannelMember channelMember = channelMemberRepository
                .findByChannelIdAndUserId(channelId, userId).orElse(null);
        if (channelMember != null
                && (channelMember.getRole() == MemberRole.OWNER
                || channelMember.getRole() == MemberRole.ADMIN)) {
            return;
        }
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AuthorizationException("Not authorized to delete this message"));
        if (teamMember.getRole() != TeamRole.OWNER && teamMember.getRole() != TeamRole.ADMIN) {
            throw new AuthorizationException("Not authorized to delete this message");
        }
    }

    /**
     * Creates or updates a read receipt for the sender when they send a message.
     *
     * @param channelId  the channel ID
     * @param userId     the user ID
     * @param messageId  the message ID to mark as read
     * @param membership the channel membership to update lastReadAt
     */
    private void upsertReadReceipt(UUID channelId, UUID userId, UUID messageId,
                                    ChannelMember membership) {
        Instant now = Instant.now();
        ReadReceipt receipt = readReceiptRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseGet(() -> ReadReceipt.builder()
                        .channelId(channelId)
                        .userId(userId)
                        .build());
        receipt.setLastReadMessageId(messageId);
        receipt.setLastReadAt(now);
        readReceiptRepository.save(receipt);

        membership.setLastReadAt(now);
        channelMemberRepository.save(membership);
    }
}
