package com.codeops.relay.service;

import com.codeops.exception.NotFoundException;
import com.codeops.relay.dto.mapper.MessageThreadMapper;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.dto.response.MessageThreadResponse;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.MessageThread;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.MessageThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing message thread metadata.
 *
 * <p>Tracks reply counts, participants, and last reply timestamps for threaded
 * conversations. Created when the first reply is posted to a root message.
 * Works alongside {@link MessageService} which delegates thread tracking here.</p>
 *
 * @see MessageThread
 * @see MessageService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ThreadService {

    private final MessageThreadRepository messageThreadRepository;
    private final MessageRepository messageRepository;
    private final MessageThreadMapper messageThreadMapper;

    /**
     * Records a reply to a root message, creating or updating the thread metadata.
     *
     * <p>If no thread exists for the parent message, one is created. The reply count
     * is incremented, last reply info is updated, and the replier is added to the
     * participant list if not already present. This is an internal method called by
     * {@link MessageService#sendMessage} — no authorization checks are performed.</p>
     *
     * @param parentMessageId the root message being replied to
     * @param channelId       the channel containing the thread
     * @param replierId       the user posting the reply
     */
    @Transactional
    public void onReply(UUID parentMessageId, UUID channelId, UUID replierId) {
        MessageThread thread = messageThreadRepository.findByRootMessageId(parentMessageId)
                .orElseGet(() -> MessageThread.builder()
                        .rootMessageId(parentMessageId)
                        .channelId(channelId)
                        .replyCount(0)
                        .participantIds("")
                        .build());

        thread.setReplyCount(thread.getReplyCount() + 1);
        thread.setLastReplyAt(Instant.now());
        thread.setLastReplyBy(replierId);
        addParticipant(thread, replierId);

        messageThreadRepository.save(thread);
    }

    /**
     * Retrieves thread metadata for a root message, if a thread exists.
     *
     * @param rootMessageId the root message ID
     * @return the thread metadata, or empty if no replies exist
     */
    @Transactional(readOnly = true)
    public Optional<MessageThread> getThreadInfo(UUID rootMessageId) {
        return messageThreadRepository.findByRootMessageId(rootMessageId);
    }

    /**
     * Retrieves full thread details including parsed participant IDs and all replies.
     *
     * <p>Replies are ordered chronologically (oldest first). The reply list contains
     * basic message data without nested reaction/attachment population — the caller
     * is responsible for enriching responses if needed.</p>
     *
     * @param rootMessageId the root message ID
     * @param userId        the requesting user (for future use in filtering)
     * @return the thread response with replies
     * @throws NotFoundException if no thread exists for the given root message
     */
    @Transactional(readOnly = true)
    public MessageThreadResponse getThread(UUID rootMessageId, UUID userId) {
        MessageThread thread = messageThreadRepository.findByRootMessageId(rootMessageId)
                .orElseThrow(() -> new NotFoundException("Thread not found for message " + rootMessageId));

        List<UUID> participantIds = parseParticipantIds(thread.getParticipantIds());
        List<Message> replies = messageRepository
                .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(rootMessageId);

        List<MessageResponse> replyResponses = replies.stream()
                .map(this::buildBasicMessageResponse)
                .toList();

        return new MessageThreadResponse(
                thread.getRootMessageId(), thread.getChannelId(),
                thread.getReplyCount(), thread.getLastReplyAt(),
                thread.getLastReplyBy(), participantIds, replyResponses);
    }

    /**
     * Retrieves all active threads in a channel, ordered by most recent reply.
     *
     * <p>Returns thread summaries without full reply lists. Each entry includes
     * reply count, last reply info, and participant IDs.</p>
     *
     * @param channelId the channel ID
     * @return list of thread summaries ordered by lastReplyAt DESC
     */
    @Transactional(readOnly = true)
    public List<MessageThreadResponse> getActiveThreads(UUID channelId) {
        List<MessageThread> threads = messageThreadRepository
                .findByChannelIdOrderByLastReplyAtDesc(channelId);

        return threads.stream()
                .map(thread -> new MessageThreadResponse(
                        thread.getRootMessageId(), thread.getChannelId(),
                        thread.getReplyCount(), thread.getLastReplyAt(),
                        thread.getLastReplyBy(),
                        parseParticipantIds(thread.getParticipantIds()),
                        List.of()))
                .toList();
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    /**
     * Parses a comma-separated string of UUIDs into a list.
     *
     * @param participantIds the comma-separated UUID string (may be null or empty)
     * @return list of UUIDs, empty list if input is null or blank
     */
    private List<UUID> parseParticipantIds(String participantIds) {
        if (participantIds == null || participantIds.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(participantIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    /**
     * Adds a user to the thread's participant list if not already present.
     *
     * @param thread the message thread
     * @param userId the user to add
     */
    private void addParticipant(MessageThread thread, UUID userId) {
        List<UUID> participants = new ArrayList<>(parseParticipantIds(thread.getParticipantIds()));
        if (!participants.contains(userId)) {
            participants.add(userId);
            thread.setParticipantIds(participants.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(",")));
        }
    }

    /**
     * Builds a basic MessageResponse without reactions, attachments, or thread info.
     *
     * @param message the message entity
     * @return a minimal message response
     */
    private MessageResponse buildBasicMessageResponse(Message message) {
        List<UUID> mentionedIds = parseMentionedUserIds(message.getMentionedUserIds());
        return new MessageResponse(
                message.getId(), message.getChannelId(), message.getSenderId(),
                null, message.getContent(), message.getMessageType(),
                message.getParentId(), message.isEdited(), message.getEditedAt(),
                message.isDeleted(), message.isMentionsEveryone(), mentionedIds,
                message.getPlatformEventId(), List.of(), List.of(), 0, null,
                message.getCreatedAt(), message.getUpdatedAt());
    }

    /**
     * Parses a comma-separated string of mentioned user IDs.
     *
     * @param mentionedUserIds the comma-separated UUID string
     * @return list of UUIDs, empty list if input is null or blank
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
}
