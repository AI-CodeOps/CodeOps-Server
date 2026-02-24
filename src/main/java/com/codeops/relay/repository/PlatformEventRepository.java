package com.codeops.relay.repository;

import com.codeops.relay.entity.PlatformEvent;
import com.codeops.relay.entity.enums.PlatformEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PlatformEvent} entities.
 *
 * <p>Provides paginated event retrieval, filtering by type and source module,
 * undelivered event queries, and event counting for analytics.</p>
 */
@Repository
public interface PlatformEventRepository extends JpaRepository<PlatformEvent, UUID> {

    /**
     * Finds all platform events for a team ordered by most recent first.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return a page of platform events
     */
    Page<PlatformEvent> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    /**
     * Finds platform events for a team filtered by event type.
     *
     * @param teamId    the team ID
     * @param eventType the event type to match
     * @return list of matching events
     */
    List<PlatformEvent> findByTeamIdAndEventType(UUID teamId, PlatformEventType eventType);

    /**
     * Finds platform events for a team filtered by source module.
     *
     * @param teamId       the team ID
     * @param sourceModule the source module name
     * @return list of matching events
     */
    List<PlatformEvent> findByTeamIdAndSourceModule(UUID teamId, String sourceModule);

    /**
     * Finds undelivered platform events for a team.
     *
     * @param teamId the team ID
     * @return list of undelivered events
     */
    List<PlatformEvent> findByTeamIdAndIsDeliveredFalse(UUID teamId);

    /**
     * Counts platform events for a team created after a given timestamp.
     *
     * @param teamId the team ID
     * @param after  the timestamp threshold
     * @return event count
     */
    long countByTeamIdAndCreatedAtAfter(UUID teamId, Instant after);
}
