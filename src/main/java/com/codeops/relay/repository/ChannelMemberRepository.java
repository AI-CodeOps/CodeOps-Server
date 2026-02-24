package com.codeops.relay.repository;

import com.codeops.relay.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChannelMember} entities.
 *
 * <p>Provides membership lookups by channel and user, plus a custom query
 * for retrieving all channel IDs a user belongs to.</p>
 */
@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

    /**
     * Finds all members of a channel.
     *
     * @param channelId the channel ID
     * @return list of channel members
     */
    List<ChannelMember> findByChannelId(UUID channelId);

    /**
     * Finds all channel memberships for a user.
     *
     * @param userId the user ID
     * @return list of memberships
     */
    List<ChannelMember> findByUserId(UUID userId);

    /**
     * Finds all non-muted channel memberships for a user.
     *
     * @param userId the user ID
     * @return list of non-muted memberships
     */
    List<ChannelMember> findByUserIdAndIsMutedFalse(UUID userId);

    /**
     * Finds a specific membership by channel and user.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     * @return the membership, if found
     */
    Optional<ChannelMember> findByChannelIdAndUserId(UUID channelId, UUID userId);

    /**
     * Checks whether a user is a member of a channel.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     * @return true if the user is a member
     */
    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    /**
     * Counts members in a channel.
     *
     * @param channelId the channel ID
     * @return member count
     */
    long countByChannelId(UUID channelId);

    /**
     * Removes a user's membership from a channel.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     */
    void deleteByChannelIdAndUserId(UUID channelId, UUID userId);

    /**
     * Retrieves all channel IDs that a user belongs to.
     *
     * @param userId the user ID
     * @return list of channel UUIDs
     */
    @Query("SELECT cm.channelId FROM ChannelMember cm WHERE cm.userId = :userId")
    List<UUID> findChannelIdsByUserId(@Param("userId") UUID userId);
}
