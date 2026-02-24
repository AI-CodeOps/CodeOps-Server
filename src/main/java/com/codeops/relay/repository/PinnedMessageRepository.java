package com.codeops.relay.repository;

import com.codeops.relay.entity.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PinnedMessage} entities.
 *
 * <p>Provides queries for listing pins in a channel, finding specific pins,
 * and counting pins for limit enforcement.</p>
 */
@Repository
public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, UUID> {

    /**
     * Finds all pinned messages in a channel ordered by most recently pinned.
     *
     * @param channelId the channel ID
     * @return list of pinned messages
     */
    List<PinnedMessage> findByChannelIdOrderByCreatedAtDesc(UUID channelId);

    /**
     * Finds a specific pin by channel and message.
     *
     * @param channelId the channel ID
     * @param messageId the message ID
     * @return the pinned message, if found
     */
    Optional<PinnedMessage> findByChannelIdAndMessageId(UUID channelId, UUID messageId);

    /**
     * Checks whether a message is pinned in a channel.
     *
     * @param channelId the channel ID
     * @param messageId the message ID
     * @return true if the message is pinned
     */
    boolean existsByChannelIdAndMessageId(UUID channelId, UUID messageId);

    /**
     * Counts pinned messages in a channel.
     *
     * @param channelId the channel ID
     * @return pin count
     */
    long countByChannelId(UUID channelId);
}
