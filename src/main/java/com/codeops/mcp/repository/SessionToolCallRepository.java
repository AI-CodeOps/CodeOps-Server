package com.codeops.mcp.repository;

import com.codeops.mcp.entity.SessionToolCall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SessionToolCall} entities.
 *
 * <p>Provides CRUD operations plus queries for listing tool calls
 * by session (ordered by time), filtering by category, counting
 * per session, and aggregating tool usage summaries.</p>
 */
@Repository
public interface SessionToolCallRepository extends JpaRepository<SessionToolCall, UUID> {

    /**
     * Lists tool calls for a session in chronological order.
     *
     * @param sessionId the session ID
     * @return ordered list of tool calls
     */
    List<SessionToolCall> findBySessionIdOrderByCalledAtAsc(UUID sessionId);

    /**
     * Lists tool calls for a session filtered by tool category.
     *
     * @param sessionId    the session ID
     * @param toolCategory the tool category to filter by
     * @return list of matching tool calls
     */
    List<SessionToolCall> findBySessionIdAndToolCategory(UUID sessionId, String toolCategory);

    /**
     * Returns a page of tool calls for a session.
     *
     * @param sessionId the session ID
     * @param pageable  pagination parameters
     * @return page of tool calls
     */
    Page<SessionToolCall> findBySessionId(UUID sessionId, Pageable pageable);

    /**
     * Counts the total number of tool calls in a session.
     *
     * @param sessionId the session ID
     * @return the count
     */
    @Query("SELECT COUNT(tc) FROM SessionToolCall tc WHERE tc.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Returns a summary of tool call counts grouped by tool name, ordered by frequency.
     *
     * @param sessionId the session ID
     * @return list of [toolName, count] pairs
     */
    @Query("SELECT tc.toolName, COUNT(tc) FROM SessionToolCall tc WHERE tc.session.id = :sessionId GROUP BY tc.toolName ORDER BY COUNT(tc) DESC")
    List<Object[]> getToolCallSummary(@Param("sessionId") UUID sessionId);
}
