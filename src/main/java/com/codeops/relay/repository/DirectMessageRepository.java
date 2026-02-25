package com.codeops.relay.repository;

import com.codeops.relay.entity.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DirectMessage} entities.
 *
 * <p>Provides paginated message retrieval, unread counts, and bulk deletion
 * for direct conversations.</p>
 */
@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    /**
     * Finds non-deleted messages in a conversation ordered by newest first.
     *
     * @param conversationId the conversation ID
     * @param pageable       pagination parameters
     * @return a page of direct messages
     */
    Page<DirectMessage> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID conversationId, Pageable pageable);

    /**
     * Counts non-deleted messages in a conversation created after a given timestamp.
     *
     * @param conversationId the conversation ID
     * @param after          the timestamp threshold
     * @return unread message count
     */
    long countByConversationIdAndCreatedAtAfterAndIsDeletedFalse(
            UUID conversationId, Instant after);

    /**
     * Counts non-deleted messages from other users in a conversation after a given timestamp.
     *
     * <p>Used for unread message counting — excludes the requesting user's own messages.</p>
     *
     * @param conversationId the conversation ID
     * @param senderId       the user ID to exclude (the requesting user)
     * @param after          the timestamp threshold
     * @return unread message count
     */
    long countByConversationIdAndSenderIdNotAndCreatedAtAfterAndIsDeletedFalse(
            UUID conversationId, UUID senderId, Instant after);

    /**
     * Counts all non-deleted messages from other users in a conversation.
     *
     * <p>Used when no read receipt exists (user has never read the conversation).</p>
     *
     * @param conversationId the conversation ID
     * @param senderId       the user ID to exclude
     * @return total message count from other participants
     */
    long countByConversationIdAndSenderIdNotAndIsDeletedFalse(
            UUID conversationId, UUID senderId);

    /**
     * Deletes all messages in a conversation (used during conversation removal).
     *
     * @param conversationId the conversation ID
     */
    void deleteByConversationId(UUID conversationId);
}
