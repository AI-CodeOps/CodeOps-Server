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
     * Deletes all messages in a conversation (used during conversation removal).
     *
     * @param conversationId the conversation ID
     */
    void deleteByConversationId(UUID conversationId);
}
