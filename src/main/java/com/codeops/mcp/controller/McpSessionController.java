package com.codeops.mcp.controller;

import com.codeops.config.AppConstants;
import com.codeops.mcp.dto.request.CompleteSessionRequest;
import com.codeops.mcp.dto.request.InitSessionRequest;
import com.codeops.mcp.dto.response.McpSessionDetailResponse;
import com.codeops.mcp.dto.response.McpSessionResponse;
import com.codeops.mcp.dto.response.ToolCallSummaryResponse;
import com.codeops.mcp.service.DeveloperProfileService;
import com.codeops.mcp.service.McpSessionService;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.repository.SessionToolCallRepository;
import com.codeops.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for MCP AI session lifecycle management.
 *
 * <p>Provides endpoints for initializing, completing, cancelling, and querying
 * MCP sessions. Session initialization requires an active developer profile
 * which is auto-created via {@link DeveloperProfileService#getOrCreateProfile}
 * if one does not exist.</p>
 *
 * <p>All endpoints require ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.MCP_API_PREFIX + "/sessions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class McpSessionController {

    private final McpSessionService mcpSessionService;
    private final DeveloperProfileService developerProfileService;
    private final SessionToolCallRepository toolCallRepository;

    /**
     * Initializes a new MCP AI development session.
     *
     * <p>Retrieves or auto-creates a developer profile for the current user
     * and team, then delegates to the session service for initialization.</p>
     *
     * @param teamId  the team ID
     * @param request the session initialization request
     * @return the initialized session detail
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public McpSessionDetailResponse initSession(@RequestParam UUID teamId,
                                                 @RequestBody @Valid InitSessionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DeveloperProfile profile = developerProfileService.getOrCreateProfile(teamId, userId);
        return mcpSessionService.initSession(profile.getId(), request);
    }

    /**
     * Completes an active session with writeback results.
     *
     * @param sessionId the session ID to complete
     * @param request   the completion request with results and metrics
     * @return the completed session detail with result
     */
    @PostMapping("/{sessionId}/complete")
    public McpSessionDetailResponse completeSession(@PathVariable UUID sessionId,
                                                      @RequestBody @Valid CompleteSessionRequest request) {
        return mcpSessionService.completeSession(sessionId, request);
    }

    /**
     * Retrieves a session by ID with full detail including tool calls and result.
     *
     * @param sessionId the session ID
     * @return the detailed session response
     */
    @GetMapping("/{sessionId}")
    public McpSessionDetailResponse getSession(@PathVariable UUID sessionId) {
        return mcpSessionService.getSession(sessionId);
    }

    /**
     * Retrieves session history for a project, ordered by most recent first.
     *
     * @param projectId the project ID
     * @param limit     maximum number of sessions to return (default 10)
     * @return the list of session responses
     */
    @GetMapping("/history")
    public List<McpSessionResponse> getSessionHistory(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "" + AppConstants.MCP_SESSION_HISTORY_LIMIT) int limit) {
        return mcpSessionService.getSessionHistory(projectId, limit);
    }

    /**
     * Retrieves paginated sessions for the current user's developer profile.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return a page of session responses
     */
    @GetMapping("/mine")
    public Page<McpSessionResponse> getMySessions(@RequestParam UUID teamId,
                                                    Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DeveloperProfile profile = developerProfileService.getOrCreateProfile(teamId, userId);
        return mcpSessionService.getDeveloperSessions(profile.getId(), pageable);
    }

    /**
     * Cancels an active session.
     *
     * @param sessionId the session ID to cancel
     * @return the updated session response
     */
    @PostMapping("/{sessionId}/cancel")
    public McpSessionResponse cancelSession(@PathVariable UUID sessionId) {
        return mcpSessionService.cancelSession(sessionId);
    }

    /**
     * Retrieves tool call summaries for a session, grouped by tool name.
     *
     * @param sessionId the session ID
     * @return list of tool call summaries with counts
     */
    @GetMapping("/{sessionId}/tool-calls")
    public List<ToolCallSummaryResponse> getToolCallSummary(@PathVariable UUID sessionId) {
        List<Object[]> summaries = toolCallRepository.getToolCallSummary(sessionId);
        return summaries.stream()
                .map(row -> new ToolCallSummaryResponse((String) row[0], (Long) row[1]))
                .toList();
    }
}
