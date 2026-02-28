package com.codeops.mcp.repository;

import com.codeops.mcp.entity.ActivityFeedEntry;
import com.codeops.mcp.entity.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ActivityFeedEntry} entities.
 *
 * <p>Provides CRUD operations plus queries for listing activity feed
 * entries by team or project (paginated, newest first), filtering
 * by time range or activity type, and looking up by session.</p>
 */
@Repository
public interface ActivityFeedEntryRepository extends JpaRepository<ActivityFeedEntry, UUID> {

    /**
     * Returns a page of activity entries for a team, newest first.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return page of activity entries
     */
    Page<ActivityFeedEntry> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    /**
     * Returns a page of activity entries for a project, newest first.
     *
     * @param projectId the project ID
     * @param pageable  pagination parameters
     * @return page of activity entries
     */
    Page<ActivityFeedEntry> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    /**
     * Lists activity entries for a team created after a given timestamp, newest first.
     *
     * @param teamId the team ID
     * @param since  the cutoff timestamp
     * @return list of recent activity entries
     */
    List<ActivityFeedEntry> findByTeamIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID teamId, Instant since);

    /**
     * Lists activity entries for a team filtered by activity type, newest first.
     *
     * @param teamId       the team ID
     * @param activityType the activity type to filter by
     * @return list of matching activity entries
     */
    List<ActivityFeedEntry> findByTeamIdAndActivityTypeOrderByCreatedAtDesc(UUID teamId, ActivityType activityType);

    /**
     * Returns a page of activity entries for a session.
     *
     * @param sessionId the session ID
     * @param pageable  pagination parameters
     * @return page of activity entries
     */
    Page<ActivityFeedEntry> findBySessionId(UUID sessionId, Pageable pageable);
}
