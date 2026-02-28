package com.codeops.mcp.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Project;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.mcp.dto.mapper.McpSessionMapper;
import com.codeops.mcp.dto.mapper.SessionResultMapper;
import com.codeops.mcp.dto.mapper.SessionToolCallMapper;
import com.codeops.mcp.dto.request.CompleteSessionRequest;
import com.codeops.mcp.dto.request.InitSessionRequest;
import com.codeops.mcp.dto.request.ToolCallRequest;
import com.codeops.mcp.dto.response.McpSessionDetailResponse;
import com.codeops.mcp.dto.response.McpSessionResponse;
import com.codeops.mcp.dto.response.SessionResultResponse;
import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.SessionResult;
import com.codeops.mcp.entity.SessionToolCall;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.entity.enums.ToolCallStatus;
import com.codeops.mcp.repository.DeveloperProfileRepository;
import com.codeops.mcp.repository.McpSessionRepository;
import com.codeops.mcp.repository.SessionResultRepository;
import com.codeops.mcp.repository.SessionToolCallRepository;
import com.codeops.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core orchestrator for MCP AI session lifecycle management.
 *
 * <p>Manages the full session lifecycle: initialize → active → complete/fail/timeout.
 * Records tool calls, stores session results, and coordinates with downstream
 * services for activity feed publishing and document management.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpSessionService {

    private final McpSessionRepository sessionRepository;
    private final SessionToolCallRepository toolCallRepository;
    private final SessionResultRepository resultRepository;
    private final DeveloperProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final McpSessionMapper sessionMapper;
    private final SessionToolCallMapper toolCallMapper;
    private final SessionResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    /**
     * Initializes a new MCP session for a developer profile and project.
     *
     * <p>Creates the session record with INITIALIZING status, transitions to ACTIVE,
     * and returns the detail response. Context assembly will be handled by
     * ContextAssemblyService when built (MCP-004).</p>
     *
     * @param developerProfileId the developer profile initiating the session
     * @param request            the session initialization request
     * @return the detailed session response
     * @throws NotFoundException if the developer profile or project is not found
     */
    @Transactional
    public McpSessionDetailResponse initSession(UUID developerProfileId, InitSessionRequest request) {
        DeveloperProfile profile = profileRepository.findById(developerProfileId)
                .orElseThrow(() -> new NotFoundException("DeveloperProfile", developerProfileId));

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project", request.projectId()));

        Environment environment = Environment.valueOf(request.environment());
        int timeout = request.timeoutMinutes() != null
                ? request.timeoutMinutes()
                : AppConstants.MCP_DEFAULT_SESSION_TIMEOUT_MINUTES;

        Instant now = Instant.now();
        McpSession session = McpSession.builder()
                .status(SessionStatus.INITIALIZING)
                .environment(environment)
                .transport(request.transport())
                .timeoutMinutes(timeout)
                .startedAt(now)
                .lastActivityAt(now)
                .developerProfile(profile)
                .project(project)
                .build();

        session = sessionRepository.save(session);
        log.info("Initialized MCP session {} for project '{}' by developer profile {}",
                session.getId(), project.getName(), developerProfileId);

        // Context assembly will be handled by ContextAssemblyService (MCP-004)

        session.setStatus(SessionStatus.ACTIVE);
        session = sessionRepository.save(session);

        return sessionMapper.toDetailResponse(session, List.of(), null);
    }

    /**
     * Completes an active session with writeback results.
     *
     * <p>Validates the session is ACTIVE, stores the session result, and transitions
     * to COMPLETED. Document updates and activity feed/Relay event publishing will
     * be integrated when downstream services are built (MCP-005, MCP-006).</p>
     *
     * @param sessionId the session to complete
     * @param request   the completion request with results
     * @return the detailed session response with result
     * @throws NotFoundException   if the session is not found
     * @throws ValidationException if the session is not active
     */
    @Transactional
    public McpSessionDetailResponse completeSession(UUID sessionId, CompleteSessionRequest request) {
        McpSession session = findSession(sessionId);
        validateActiveSession(session);

        session.setStatus(SessionStatus.COMPLETING);

        SessionResult result = SessionResult.builder()
                .summary(request.summary())
                .commitHashesJson(serializeJson(request.commitHashes()))
                .filesChangedJson(serializeJson(request.filesChanged()))
                .endpointsChangedJson(serializeJson(request.endpointsChanged()))
                .testsAdded(request.testsAdded() != null ? request.testsAdded() : 0)
                .testCoverage(request.testCoverage())
                .linesAdded(request.linesAdded() != null ? request.linesAdded() : 0)
                .linesRemoved(request.linesRemoved() != null ? request.linesRemoved() : 0)
                .dependencyChangesJson(request.dependencyChanges())
                .durationMinutes(request.durationMinutes() != null ? request.durationMinutes() : 0)
                .tokenUsage(request.tokenUsage())
                .session(session)
                .build();

        result = resultRepository.save(result);
        session.setResult(result);

        // Document updates will be handled by DocumentManagementService (MCP-005)
        if (request.documentUpdates() != null && !request.documentUpdates().isEmpty()) {
            log.info("Session {} has {} document updates (DocumentManagementService pending MCP-005)",
                    sessionId, request.documentUpdates().size());
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session = sessionRepository.save(session);

        // Activity feed and Relay event publishing pending MCP-006
        log.info("Completed MCP session {} with summary: {}", sessionId, request.summary());

        List<SessionToolCallResponse> toolCalls = toolCallMapper.toResponseList(
                toolCallRepository.findBySessionIdOrderByCalledAtAsc(sessionId));
        SessionResultResponse resultResponse = resultMapper.toResponse(result);

        return sessionMapper.toDetailResponse(session, toolCalls, resultResponse);
    }

    /**
     * Records a tool call within an active session.
     *
     * <p>Creates the tool call record and updates the session's lastActivityAt
     * timestamp and totalToolCalls counter.</p>
     *
     * @param sessionId    the session the tool call belongs to
     * @param request      the tool call request details
     * @param status       the outcome status of the tool call
     * @param durationMs   execution duration in milliseconds
     * @param responseJson the JSON response from the tool
     * @param errorMessage error message if the tool call failed
     * @return the tool call response
     * @throws NotFoundException   if the session is not found
     * @throws ValidationException if the session is not active
     */
    @Transactional
    public SessionToolCallResponse recordToolCall(UUID sessionId, ToolCallRequest request,
                                                  ToolCallStatus status, long durationMs,
                                                  String responseJson, String errorMessage) {
        McpSession session = findSession(sessionId);
        validateActiveSession(session);

        SessionToolCall toolCall = SessionToolCall.builder()
                .toolName(request.toolName())
                .toolCategory(request.toolCategory())
                .requestJson(request.argumentsJson())
                .responseJson(responseJson)
                .status(status)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .calledAt(Instant.now())
                .session(session)
                .build();

        toolCall = toolCallRepository.save(toolCall);

        session.setLastActivityAt(Instant.now());
        session.setTotalToolCalls(session.getTotalToolCalls() + 1);
        sessionRepository.save(session);

        log.debug("Recorded tool call {} ({}) for session {}, status: {}",
                request.toolName(), request.toolCategory(), sessionId, status);

        return toolCallMapper.toResponse(toolCall);
    }

    /**
     * Retrieves a session by ID with full detail including tool calls and result.
     *
     * @param sessionId the session ID
     * @return the detailed session response
     * @throws NotFoundException if the session is not found
     */
    @Transactional(readOnly = true)
    public McpSessionDetailResponse getSession(UUID sessionId) {
        McpSession session = findSession(sessionId);

        List<SessionToolCallResponse> toolCalls = toolCallMapper.toResponseList(
                toolCallRepository.findBySessionIdOrderByCalledAtAsc(sessionId));

        SessionResultResponse resultResponse = session.getResult() != null
                ? resultMapper.toResponse(session.getResult())
                : null;

        return sessionMapper.toDetailResponse(session, toolCalls, resultResponse);
    }

    /**
     * Retrieves session history for a project, ordered by most recent first.
     *
     * @param projectId the project ID
     * @param limit     maximum number of sessions to return
     * @return the list of session responses
     */
    @Transactional(readOnly = true)
    public List<McpSessionResponse> getSessionHistory(UUID projectId, int limit) {
        Page<McpSession> page = sessionRepository.findByProjectId(
                projectId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return sessionMapper.toResponseList(page.getContent());
    }

    /**
     * Retrieves paginated sessions for a developer profile.
     *
     * @param developerProfileId the developer profile ID
     * @param pageable           pagination parameters
     * @return a page of session responses
     */
    @Transactional(readOnly = true)
    public Page<McpSessionResponse> getDeveloperSessions(UUID developerProfileId, Pageable pageable) {
        Page<McpSession> page = sessionRepository.findByDeveloperProfileId(developerProfileId, pageable);
        return page.map(sessionMapper::toResponse);
    }

    /**
     * Cancels an active session.
     *
     * @param sessionId the session to cancel
     * @return the updated session response
     * @throws NotFoundException   if the session is not found
     * @throws ValidationException if the session is not active
     */
    @Transactional
    public McpSessionResponse cancelSession(UUID sessionId) {
        McpSession session = findSession(sessionId);
        validateActiveSession(session);

        session.setStatus(SessionStatus.CANCELLED);
        session.setCompletedAt(Instant.now());
        session = sessionRepository.save(session);

        log.info("Cancelled MCP session {}", sessionId);
        return sessionMapper.toResponse(session);
    }

    /**
     * Times out expired active sessions whose last activity exceeds their timeout.
     *
     * <p>Called by the Spring scheduler every 60 seconds. Finds all ACTIVE sessions
     * and checks each against its individual timeout configuration.</p>
     *
     * @return the number of sessions that were timed out
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public int timeoutExpiredSessions() {
        List<McpSession> activeSessions = sessionRepository.findByStatus(SessionStatus.ACTIVE);
        Instant now = Instant.now();
        int count = 0;

        for (McpSession session : activeSessions) {
            Instant lastActivity = session.getLastActivityAt() != null
                    ? session.getLastActivityAt()
                    : session.getCreatedAt();
            Instant expiresAt = lastActivity.plusSeconds(session.getTimeoutMinutes() * 60L);

            if (expiresAt.isBefore(now)) {
                session.setStatus(SessionStatus.TIMED_OUT);
                session.setCompletedAt(now);
                session.setErrorMessage("Session timed out after "
                        + session.getTimeoutMinutes() + " minutes of inactivity");
                sessionRepository.save(session);
                log.warn("Session {} timed out (last activity: {})", session.getId(), lastActivity);
                count++;
            }
        }

        if (count > 0) {
            log.info("Timed out {} expired MCP sessions", count);
        }
        return count;
    }

    /**
     * Returns the count of active sessions for a developer profile.
     *
     * @param developerProfileId the developer profile ID
     * @return the number of active sessions
     */
    @Transactional(readOnly = true)
    public long getActiveSessionCount(UUID developerProfileId) {
        return sessionRepository.countByDeveloperProfileIdAndStatus(
                developerProfileId, SessionStatus.ACTIVE);
    }

    // ── Private Helpers ──

    private McpSession findSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("McpSession", sessionId));
    }

    private void validateActiveSession(McpSession session) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ValidationException("Session " + session.getId()
                    + " is not active. Current status: " + session.getStatus());
        }
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize value to JSON", e);
            return null;
        }
    }
}
