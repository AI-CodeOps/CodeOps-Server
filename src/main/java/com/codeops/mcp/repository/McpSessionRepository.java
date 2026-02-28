package com.codeops.mcp.repository;

import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link McpSession} entities.
 *
 * <p>Provides CRUD operations plus queries for listing sessions by
 * developer profile or project, filtering by status, detecting
 * timed-out sessions, and computing aggregate counts.</p>
 */
@Repository
public interface McpSessionRepository extends JpaRepository<McpSession, UUID> {

    /**
     * Lists sessions for a developer profile, newest first.
     *
     * @param profileId the developer profile ID
     * @return ordered list of sessions
     */
    List<McpSession> findByDeveloperProfileIdOrderByCreatedAtDesc(UUID profileId);

    /**
     * Lists sessions for a project, newest first.
     *
     * @param projectId the project ID
     * @return ordered list of sessions
     */
    List<McpSession> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Lists sessions for a project filtered by status, newest first.
     *
     * @param projectId the project ID
     * @param status    the session status to filter by
     * @return ordered list of sessions
     */
    List<McpSession> findByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, SessionStatus status);

    /**
     * Returns a page of sessions for a developer profile.
     *
     * @param profileId the developer profile ID
     * @param pageable  pagination parameters
     * @return page of sessions
     */
    Page<McpSession> findByDeveloperProfileId(UUID profileId, Pageable pageable);

    /**
     * Returns a page of sessions for a project.
     *
     * @param projectId the project ID
     * @param pageable  pagination parameters
     * @return page of sessions
     */
    Page<McpSession> findByProjectId(UUID projectId, Pageable pageable);

    /**
     * Lists all sessions with a given status.
     *
     * @param status the session status
     * @return list of sessions
     */
    List<McpSession> findByStatus(SessionStatus status);

    /**
     * Finds sessions that may have timed out (active but no recent activity).
     *
     * @param status the session status to filter by
     * @param cutoff the timestamp cutoff for last activity
     * @return list of potentially timed-out sessions
     */
    List<McpSession> findByStatusAndLastActivityAtBefore(SessionStatus status, Instant cutoff);

    /**
     * Counts sessions for a developer profile with a given status.
     *
     * @param profileId the developer profile ID
     * @param status    the session status
     * @return the count
     */
    @Query("SELECT COUNT(s) FROM McpSession s WHERE s.developerProfile.id = :profileId AND s.status = :status")
    long countByDeveloperProfileIdAndStatus(@Param("profileId") UUID profileId, @Param("status") SessionStatus status);

    /**
     * Finds the most recently completed session for a project with a given status.
     *
     * @param projectId the project ID
     * @param status    the session status
     * @return the most recent matching session
     */
    Optional<McpSession> findTopByProjectIdAndStatusOrderByCompletedAtDesc(UUID projectId, SessionStatus status);
}
