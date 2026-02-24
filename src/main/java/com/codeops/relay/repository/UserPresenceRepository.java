package com.codeops.relay.repository;

import com.codeops.relay.entity.UserPresence;
import com.codeops.relay.entity.enums.PresenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserPresence} entities.
 *
 * <p>Provides presence lookups by user/team, status filtering,
 * and a custom query for detecting stale online users whose
 * heartbeat has expired.</p>
 */
@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, UUID> {

    /**
     * Finds a user's presence record within a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the presence record, if found
     */
    Optional<UserPresence> findByUserIdAndTeamId(UUID userId, UUID teamId);

    /**
     * Finds all presence records for a team.
     *
     * @param teamId the team ID
     * @return list of presence records
     */
    List<UserPresence> findByTeamId(UUID teamId);

    /**
     * Finds presence records for a team filtered by status.
     *
     * @param teamId the team ID
     * @param status the presence status to match
     * @return list of matching presence records
     */
    List<UserPresence> findByTeamIdAndStatus(UUID teamId, PresenceStatus status);

    /**
     * Finds presence records for a team excluding a given status.
     *
     * @param teamId the team ID
     * @param status the presence status to exclude
     * @return list of non-matching presence records
     */
    List<UserPresence> findByTeamIdAndStatusNot(UUID teamId, PresenceStatus status);

    /**
     * Finds users marked as ONLINE whose last heartbeat is older than the cutoff,
     * indicating they should be transitioned to OFFLINE.
     *
     * @param cutoff the heartbeat expiration threshold
     * @return list of stale online users
     */
    @Query("SELECT up FROM UserPresence up WHERE up.status = 'ONLINE' "
            + "AND up.lastHeartbeatAt < :cutoff")
    List<UserPresence> findStaleOnlineUsers(@Param("cutoff") Instant cutoff);
}
