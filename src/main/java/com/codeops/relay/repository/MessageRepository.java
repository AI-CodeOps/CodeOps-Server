package com.codeops.relay.repository;

import com.codeops.relay.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Message} entities.
 *
 * <p>Provides paginated message retrieval, thread queries, unread counts,
 * and full-text search within and across channels.</p>
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Finds non-deleted messages in a channel ordered by newest first.
     *
     * @param channelId the channel ID
     * @param pageable  pagination parameters
     * @return a page of messages
     */
    Page<Message> findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID channelId, Pageable pageable);

    /**
     * Finds non-deleted top-level messages (no parent) in a channel.
     *
     * @param channelId the channel ID
     * @param pageable  pagination parameters
     * @return a page of top-level messages
     */
    Page<Message> findByChannelIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID channelId, Pageable pageable);

    /**
     * Finds non-deleted replies to a parent message ordered chronologically.
     *
     * @param parentId the parent message ID
     * @return list of thread replies
     */
    List<Message> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId);

    /**
     * Counts all messages in a channel created after a given timestamp.
     *
     * @param channelId the channel ID
     * @param after     the timestamp threshold
     * @return message count
     */
    long countByChannelIdAndCreatedAtAfter(UUID channelId, Instant after);

    /**
     * Counts non-deleted messages in a channel created after a given timestamp.
     *
     * @param channelId the channel ID
     * @param after     the timestamp threshold
     * @return non-deleted message count
     */
    long countByChannelIdAndCreatedAtAfterAndIsDeletedFalse(UUID channelId, Instant after);

    /**
     * Counts unread (non-deleted) messages in a channel since a given timestamp.
     *
     * @param channelId the channel ID
     * @param since     the last-read timestamp
     * @return unread message count
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.channelId = :channelId "
            + "AND m.createdAt > :since AND m.isDeleted = false")
    long countUnreadMessages(@Param("channelId") UUID channelId, @Param("since") Instant since);

    /**
     * Searches non-deleted messages within a single channel by content substring.
     *
     * @param channelId the channel ID
     * @param query     the search term
     * @param pageable  pagination parameters
     * @return a page of matching messages
     */
    @Query("SELECT m FROM Message m WHERE m.channelId = :channelId AND m.isDeleted = false "
            + "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.createdAt DESC")
    Page<Message> searchInChannel(@Param("channelId") UUID channelId,
                                  @Param("query") String query, Pageable pageable);

    /**
     * Searches non-deleted messages across multiple channels by content substring.
     *
     * @param channelIds the channel IDs to search
     * @param query      the search term
     * @param pageable   pagination parameters
     * @return a page of matching messages
     */
    @Query("SELECT m FROM Message m WHERE m.channelId IN :channelIds AND m.isDeleted = false "
            + "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.createdAt DESC")
    Page<Message> searchAcrossChannels(@Param("channelIds") List<UUID> channelIds,
                                       @Param("query") String query, Pageable pageable);

    /**
     * Deletes all messages in a channel (hard delete, used during channel removal).
     *
     * @param channelId the channel ID
     */
    void deleteByChannelId(UUID channelId);
}
