package com.codeops.mcp.repository;

import com.codeops.mcp.entity.SessionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SessionResult} entities.
 *
 * <p>Provides CRUD operations plus lookup by session ID
 * for the one-to-one session-to-result relationship.</p>
 */
@Repository
public interface SessionResultRepository extends JpaRepository<SessionResult, UUID> {

    /**
     * Finds the result for a given session.
     *
     * @param sessionId the session ID
     * @return the session result if found
     */
    Optional<SessionResult> findBySessionId(UUID sessionId);
}
