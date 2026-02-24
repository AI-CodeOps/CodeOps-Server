package com.codeops.relay.repository;

import com.codeops.relay.entity.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ReadReceipt} entities.
 *
 * <p>Provides queries for finding a user's read position in a channel
 * and bulk deletion during channel cleanup.</p>
 */
@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, UUID> {

    /**
     * Finds the read receipt for a user in a specific channel.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     * @return the read receipt, if found
     */
    Optional<ReadReceipt> findByChannelIdAndUserId(UUID channelId, UUID userId);

    /**
     * Finds all read receipts for a channel.
     *
     * @param channelId the channel ID
     * @return list of read receipts
     */
    List<ReadReceipt> findByChannelId(UUID channelId);

    /**
     * Deletes all read receipts for a channel (used during channel removal).
     *
     * @param channelId the channel ID
     */
    void deleteByChannelId(UUID channelId);
}
