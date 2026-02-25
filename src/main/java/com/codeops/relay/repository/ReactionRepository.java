package com.codeops.relay.repository;

import com.codeops.relay.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Reaction} entities.
 *
 * <p>Provides lookups for reactions on channel messages and direct messages,
 * uniqueness checks, and an aggregation query for reaction counts by emoji.</p>
 */
@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    /**
     * Finds all reactions on a channel message.
     *
     * @param messageId the channel message ID
     * @return list of reactions
     */
    List<Reaction> findByMessageId(UUID messageId);

    /**
     * Finds all reactions on a direct message.
     *
     * @param directMessageId the direct message ID
     * @return list of reactions
     */
    List<Reaction> findByDirectMessageId(UUID directMessageId);

    /**
     * Finds a specific reaction by a user on a channel message.
     *
     * @param messageId the channel message ID
     * @param userId    the user ID
     * @param emoji     the emoji string
     * @return the reaction, if found
     */
    Optional<Reaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);

    /**
     * Finds a specific reaction by a user on a direct message.
     *
     * @param directMessageId the direct message ID
     * @param userId          the user ID
     * @param emoji           the emoji string
     * @return the reaction, if found
     */
    Optional<Reaction> findByDirectMessageIdAndUserIdAndEmoji(
            UUID directMessageId, UUID userId, String emoji);

    /**
     * Counts reactions with a specific emoji on a channel message.
     *
     * @param messageId the channel message ID
     * @param emoji     the emoji string
     * @return reaction count
     */
    long countByMessageIdAndEmoji(UUID messageId, String emoji);

    /**
     * Deletes all reactions on a channel message.
     *
     * @param messageId the channel message ID
     */
    void deleteByMessageId(UUID messageId);

    /**
     * Deletes all reactions on a direct message.
     *
     * @param directMessageId the direct message ID
     */
    void deleteByDirectMessageId(UUID directMessageId);

    /**
     * Finds all reactions by a user on messages in a specific channel.
     *
     * @param userId    the user ID
     * @param channelId the channel ID (traverses via message relationship)
     * @return list of reactions
     */
    @Query("SELECT r FROM Reaction r JOIN r.message m WHERE r.userId = :userId AND m.channelId = :channelId")
    List<Reaction> findByUserIdAndMessageChannelId(@Param("userId") UUID userId,
                                                    @Param("channelId") UUID channelId);

    /**
     * Counts reactions grouped by emoji for a channel message, ordered by popularity.
     *
     * @param messageId the channel message ID
     * @return list of [emoji, count] pairs
     */
    @Query("SELECT r.emoji, COUNT(r) FROM Reaction r WHERE r.messageId = :messageId "
            + "GROUP BY r.emoji ORDER BY COUNT(r) DESC")
    List<Object[]> countReactionsByMessageId(@Param("messageId") UUID messageId);
}
