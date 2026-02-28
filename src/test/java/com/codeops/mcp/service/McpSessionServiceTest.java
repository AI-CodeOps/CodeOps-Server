package com.codeops.mcp.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
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
import com.codeops.mcp.entity.enums.*;
import com.codeops.mcp.repository.DeveloperProfileRepository;
import com.codeops.mcp.repository.McpSessionRepository;
import com.codeops.mcp.repository.SessionResultRepository;
import com.codeops.mcp.repository.SessionToolCallRepository;
import com.codeops.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpSessionService}.
 *
 * <p>Covers session lifecycle (init, complete, cancel, timeout), tool call recording,
 * query methods, and edge cases with validation and not-found scenarios.</p>
 */
@ExtendWith(MockitoExtension.class)
class McpSessionServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock
    private McpSessionRepository sessionRepository;
    @Mock
    private SessionToolCallRepository toolCallRepository;
    @Mock
    private SessionResultRepository resultRepository;
    @Mock
    private DeveloperProfileRepository profileRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private McpSessionMapper sessionMapper;
    @Mock
    private SessionToolCallMapper toolCallMapper;
    @Mock
    private SessionResultMapper resultMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private McpSessionService service;

    // ── Test Data Builders ──

    private User createUser() {
        User user = User.builder()
                .displayName("Test Developer")
                .email("test@test.com")
                .passwordHash("hash")
                .build();
        user.setId(USER_ID);
        return user;
    }

    private Team createTeam() {
        Team team = Team.builder()
                .name("Test Team")
                .owner(createUser())
                .build();
        team.setId(TEAM_ID);
        return team;
    }

    private Project createProject() {
        Project project = Project.builder()
                .name("Test Project")
                .team(createTeam())
                .createdBy(createUser())
                .build();
        project.setId(PROJECT_ID);
        return project;
    }

    private DeveloperProfile createProfile() {
        DeveloperProfile profile = DeveloperProfile.builder()
                .displayName("Test Dev")
                .team(createTeam())
                .user(createUser())
                .build();
        profile.setId(PROFILE_ID);
        return profile;
    }

    private McpSession createActiveSession() {
        McpSession session = McpSession.builder()
                .status(SessionStatus.ACTIVE)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .timeoutMinutes(120)
                .startedAt(NOW)
                .lastActivityAt(NOW)
                .totalToolCalls(0)
                .developerProfile(createProfile())
                .project(createProject())
                .build();
        session.setId(SESSION_ID);
        session.setCreatedAt(NOW);
        session.setUpdatedAt(NOW);
        return session;
    }

    private McpSessionDetailResponse createDetailResponse() {
        return new McpSessionDetailResponse(
                SESSION_ID, SessionStatus.ACTIVE, "Test Project", "Test Developer",
                Environment.DEVELOPMENT, McpTransport.SSE, NOW, null, NOW,
                120, 0, null, List.of(), null, NOW, NOW);
    }

    private McpSessionResponse createSessionResponse() {
        return new McpSessionResponse(
                SESSION_ID, SessionStatus.ACTIVE, "Test Project", "Test Developer",
                Environment.DEVELOPMENT, McpTransport.SSE, NOW, null, 0, NOW);
    }

    // ── initSession ──

    @Nested
    @DisplayName("initSession")
    class InitSessionTests {

        @Test
        @DisplayName("creates session and returns detail response")
        void initSession_createsSessionAndReturnsDetail() {
            DeveloperProfile profile = createProfile();
            Project project = createProject();
            McpSessionDetailResponse expectedResponse = createDetailResponse();

            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            List<SessionStatus> statusesAtSave = new java.util.ArrayList<>();
            when(sessionRepository.save(any(McpSession.class))).thenAnswer(invocation -> {
                McpSession s = invocation.getArgument(0);
                statusesAtSave.add(s.getStatus());
                if (s.getId() == null) {
                    s.setId(SESSION_ID);
                }
                return s;
            });
            when(sessionMapper.toDetailResponse(any(McpSession.class), eq(List.of()), eq(null)))
                    .thenReturn(expectedResponse);

            InitSessionRequest request = new InitSessionRequest(
                    PROJECT_ID, "DEVELOPMENT", McpTransport.SSE, null);

            McpSessionDetailResponse response = service.initSession(PROFILE_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(SESSION_ID);

            // Verify status transition: INITIALIZING → ACTIVE across two saves
            assertThat(statusesAtSave).containsExactly(
                    SessionStatus.INITIALIZING, SessionStatus.ACTIVE);

            // Verify the session passed to the mapper for final response
            ArgumentCaptor<McpSession> captor = ArgumentCaptor.forClass(McpSession.class);
            verify(sessionMapper).toDetailResponse(captor.capture(), eq(List.of()), eq(null));
            McpSession finalSession = captor.getValue();
            assertThat(finalSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(finalSession.getTimeoutMinutes())
                    .isEqualTo(AppConstants.MCP_DEFAULT_SESSION_TIMEOUT_MINUTES);
            assertThat(finalSession.getStartedAt()).isNotNull();
            assertThat(finalSession.getLastActivityAt()).isNotNull();
        }

        @Test
        @DisplayName("throws NotFoundException for invalid project")
        void initSession_invalidProject_throwsNotFound() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(createProfile()));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            InitSessionRequest request = new InitSessionRequest(
                    PROJECT_ID, "DEVELOPMENT", McpTransport.SSE, null);

            assertThatThrownBy(() -> service.initSession(PROFILE_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Project");
        }

        @Test
        @DisplayName("throws NotFoundException for invalid profile")
        void initSession_invalidProfile_throwsNotFound() {
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

            InitSessionRequest request = new InitSessionRequest(
                    PROJECT_ID, "DEVELOPMENT", McpTransport.SSE, null);

            assertThatThrownBy(() -> service.initSession(PROFILE_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("DeveloperProfile");
        }
    }

    // ── completeSession ──

    @Nested
    @DisplayName("completeSession")
    class CompleteSessionTests {

        @Test
        @DisplayName("stores result and updates status to COMPLETED")
        void completeSession_storesResultAndUpdatesStatus() {
            McpSession session = createActiveSession();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(resultRepository.save(any(SessionResult.class))).thenAnswer(invocation -> {
                SessionResult r = invocation.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(sessionRepository.save(any(McpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(toolCallRepository.findBySessionIdOrderByCalledAtAsc(SESSION_ID))
                    .thenReturn(Collections.emptyList());
            when(toolCallMapper.toResponseList(Collections.emptyList())).thenReturn(List.of());
            when(resultMapper.toResponse(any(SessionResult.class)))
                    .thenReturn(new SessionResultResponse(
                            UUID.randomUUID(), "Auth done", null, null, null,
                            5, 92.5, 200, 50, null, 45, 150000L, NOW));
            when(sessionMapper.toDetailResponse(any(McpSession.class), any(), any()))
                    .thenReturn(createDetailResponse());

            CompleteSessionRequest request = new CompleteSessionRequest(
                    "Auth done", List.of("abc123"), null, null,
                    5, 92.5, 200, 50, null, 45, 150000L, null);

            McpSessionDetailResponse response = service.completeSession(SESSION_ID, request);

            assertThat(response).isNotNull();

            ArgumentCaptor<SessionResult> resultCaptor = ArgumentCaptor.forClass(SessionResult.class);
            verify(resultRepository).save(resultCaptor.capture());
            assertThat(resultCaptor.getValue().getSummary()).isEqualTo("Auth done");
            assertThat(resultCaptor.getValue().getTestsAdded()).isEqualTo(5);

            ArgumentCaptor<McpSession> sessionCaptor = ArgumentCaptor.forClass(McpSession.class);
            verify(sessionRepository).save(sessionCaptor.capture());
            McpSession savedSession = sessionCaptor.getValue();
            assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
            assertThat(savedSession.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws ValidationException when session is not active")
        void completeSession_notActive_throwsValidation() {
            McpSession session = createActiveSession();
            session.setStatus(SessionStatus.COMPLETED);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            CompleteSessionRequest request = new CompleteSessionRequest(
                    "Done", null, null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> service.completeSession(SESSION_ID, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("logs document updates when present")
        void completeSession_withDocumentUpdates_updatesDocuments() {
            McpSession session = createActiveSession();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(resultRepository.save(any(SessionResult.class))).thenAnswer(invocation -> {
                SessionResult r = invocation.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(sessionRepository.save(any(McpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(toolCallRepository.findBySessionIdOrderByCalledAtAsc(SESSION_ID))
                    .thenReturn(Collections.emptyList());
            when(toolCallMapper.toResponseList(Collections.emptyList())).thenReturn(List.of());
            when(resultMapper.toResponse(any(SessionResult.class)))
                    .thenReturn(new SessionResultResponse(
                            UUID.randomUUID(), "Updated docs", null, null, null,
                            0, null, 0, 0, null, 0, null, NOW));
            when(sessionMapper.toDetailResponse(any(McpSession.class), any(), any()))
                    .thenReturn(createDetailResponse());

            List<CompleteSessionRequest.DocumentUpdate> docUpdates = List.of(
                    new CompleteSessionRequest.DocumentUpdate(
                            DocumentType.CLAUDE_MD, "# Updated", "Refreshed project docs"));

            CompleteSessionRequest request = new CompleteSessionRequest(
                    "Updated docs", null, null, null, null, null, null, null, null, null, null, docUpdates);

            McpSessionDetailResponse response = service.completeSession(SESSION_ID, request);

            assertThat(response).isNotNull();
            verify(resultRepository).save(any(SessionResult.class));
        }
    }

    // ── recordToolCall ──

    @Nested
    @DisplayName("recordToolCall")
    class RecordToolCallTests {

        @Test
        @DisplayName("creates tool call entry and updates session counters")
        void recordToolCall_createsEntryAndUpdatesSession() {
            McpSession session = createActiveSession();
            session.setTotalToolCalls(2);

            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(toolCallRepository.save(any(SessionToolCall.class))).thenAnswer(invocation -> {
                SessionToolCall tc = invocation.getArgument(0);
                tc.setId(UUID.randomUUID());
                return tc;
            });
            when(sessionRepository.save(any(McpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SessionToolCallResponse expectedResponse = new SessionToolCallResponse(
                    UUID.randomUUID(), "registry.list", "registry",
                    "{}", "{\"services\":[]}", ToolCallStatus.SUCCESS, 100, null, NOW, NOW);
            when(toolCallMapper.toResponse(any(SessionToolCall.class))).thenReturn(expectedResponse);

            ToolCallRequest request = new ToolCallRequest("registry.list", "registry", "{}");

            SessionToolCallResponse response = service.recordToolCall(
                    SESSION_ID, request, ToolCallStatus.SUCCESS, 100L, "{\"services\":[]}", null);

            assertThat(response).isNotNull();
            assertThat(response.toolName()).isEqualTo("registry.list");

            ArgumentCaptor<SessionToolCall> tcCaptor = ArgumentCaptor.forClass(SessionToolCall.class);
            verify(toolCallRepository).save(tcCaptor.capture());
            assertThat(tcCaptor.getValue().getToolName()).isEqualTo("registry.list");
            assertThat(tcCaptor.getValue().getDurationMs()).isEqualTo(100L);

            ArgumentCaptor<McpSession> sessionCaptor = ArgumentCaptor.forClass(McpSession.class);
            verify(sessionRepository).save(sessionCaptor.capture());
            assertThat(sessionCaptor.getValue().getTotalToolCalls()).isEqualTo(3);
            assertThat(sessionCaptor.getValue().getLastActivityAt()).isNotNull();
        }

        @Test
        @DisplayName("throws ValidationException for inactive session")
        void recordToolCall_inactiveSession_throwsValidation() {
            McpSession session = createActiveSession();
            session.setStatus(SessionStatus.COMPLETED);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            ToolCallRequest request = new ToolCallRequest("registry.list", "registry", null);

            assertThatThrownBy(() -> service.recordToolCall(
                    SESSION_ID, request, ToolCallStatus.SUCCESS, 100L, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not active");
        }
    }

    // ── getSession ──

    @Nested
    @DisplayName("getSession")
    class GetSessionTests {

        @Test
        @DisplayName("returns detail response with tool calls and result")
        void getSession_returnsDetailWithToolCallsAndResult() {
            McpSession session = createActiveSession();
            SessionResult result = SessionResult.builder()
                    .summary("Auth module done")
                    .session(session)
                    .build();
            result.setId(UUID.randomUUID());
            session.setResult(result);

            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            List<SessionToolCall> toolCalls = List.of(
                    SessionToolCall.builder()
                            .toolName("registry.list")
                            .toolCategory("registry")
                            .status(ToolCallStatus.SUCCESS)
                            .durationMs(100)
                            .calledAt(NOW)
                            .session(session)
                            .build());
            when(toolCallRepository.findBySessionIdOrderByCalledAtAsc(SESSION_ID)).thenReturn(toolCalls);

            List<SessionToolCallResponse> toolCallResponses = List.of(
                    new SessionToolCallResponse(UUID.randomUUID(), "registry.list", "registry",
                            null, null, ToolCallStatus.SUCCESS, 100, null, NOW, NOW));
            when(toolCallMapper.toResponseList(toolCalls)).thenReturn(toolCallResponses);

            SessionResultResponse resultResponse = new SessionResultResponse(
                    result.getId(), "Auth module done", null, null, null,
                    0, null, 0, 0, null, 0, null, NOW);
            when(resultMapper.toResponse(result)).thenReturn(resultResponse);

            McpSessionDetailResponse expectedResponse = new McpSessionDetailResponse(
                    SESSION_ID, SessionStatus.ACTIVE, "Test Project", "Test Developer",
                    Environment.DEVELOPMENT, McpTransport.SSE, NOW, null, NOW,
                    120, 0, null, toolCallResponses, resultResponse, NOW, NOW);
            when(sessionMapper.toDetailResponse(session, toolCallResponses, resultResponse))
                    .thenReturn(expectedResponse);

            McpSessionDetailResponse response = service.getSession(SESSION_ID);

            assertThat(response).isNotNull();
            assertThat(response.toolCalls()).hasSize(1);
            assertThat(response.result()).isNotNull();
            assertThat(response.result().summary()).isEqualTo("Auth module done");
        }

        @Test
        @DisplayName("throws NotFoundException for unknown session ID")
        void getSession_notFound_throwsNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(sessionRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSession(unknownId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("McpSession");
        }
    }

    // ── getSessionHistory ──

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistoryTests {

        @Test
        @DisplayName("returns ordered list limited by count")
        void getSessionHistory_returnsOrderedList() {
            McpSession session = createActiveSession();
            Page<McpSession> page = new PageImpl<>(List.of(session));

            when(sessionRepository.findByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(page);

            McpSessionResponse sessionResponse = createSessionResponse();
            when(sessionMapper.toResponseList(List.of(session))).thenReturn(List.of(sessionResponse));

            List<McpSessionResponse> result = service.getSessionHistory(PROJECT_ID, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(SESSION_ID);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(sessionRepository).findByProjectId(eq(PROJECT_ID), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }
    }

    // ── getDeveloperSessions ──

    @Nested
    @DisplayName("getDeveloperSessions")
    class GetDeveloperSessionsTests {

        @Test
        @DisplayName("returns paginated session responses")
        void getDeveloperSessions_returnsPaged() {
            McpSession session = createActiveSession();
            Page<McpSession> page = new PageImpl<>(List.of(session));
            Pageable pageable = PageRequest.of(0, 20);

            when(sessionRepository.findByDeveloperProfileId(PROFILE_ID, pageable)).thenReturn(page);

            McpSessionResponse sessionResponse = createSessionResponse();
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

            Page<McpSessionResponse> result = service.getDeveloperSessions(PROFILE_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(SESSION_ID);
        }
    }

    // ── cancelSession ──

    @Nested
    @DisplayName("cancelSession")
    class CancelSessionTests {

        @Test
        @DisplayName("transitions active session to CANCELLED")
        void cancelSession_updatesStatus() {
            McpSession session = createActiveSession();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(McpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            McpSessionResponse expectedResponse = new McpSessionResponse(
                    SESSION_ID, SessionStatus.CANCELLED, "Test Project", "Test Developer",
                    Environment.DEVELOPMENT, McpTransport.SSE, NOW, NOW, 0, NOW);
            when(sessionMapper.toResponse(any(McpSession.class))).thenReturn(expectedResponse);

            McpSessionResponse response = service.cancelSession(SESSION_ID);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(SessionStatus.CANCELLED);

            ArgumentCaptor<McpSession> captor = ArgumentCaptor.forClass(McpSession.class);
            verify(sessionRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SessionStatus.CANCELLED);
            assertThat(captor.getValue().getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws ValidationException for already completed session")
        void cancelSession_notActive_throwsValidation() {
            McpSession session = createActiveSession();
            session.setStatus(SessionStatus.COMPLETED);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.cancelSession(SESSION_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not active");
        }
    }

    // ── timeoutExpiredSessions ──

    @Nested
    @DisplayName("timeoutExpiredSessions")
    class TimeoutTests {

        @Test
        @DisplayName("times out sessions past their timeout threshold")
        void timeoutExpiredSessions_timeoutsCorrectSessions() {
            McpSession expiredSession = createActiveSession();
            expiredSession.setTimeoutMinutes(30);
            expiredSession.setLastActivityAt(NOW.minus(60, ChronoUnit.MINUTES));

            McpSession freshSession = createActiveSession();
            freshSession.setId(UUID.randomUUID());
            freshSession.setTimeoutMinutes(120);
            freshSession.setLastActivityAt(NOW.minus(10, ChronoUnit.MINUTES));

            when(sessionRepository.findByStatus(SessionStatus.ACTIVE))
                    .thenReturn(List.of(expiredSession, freshSession));
            when(sessionRepository.save(any(McpSession.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            int count = service.timeoutExpiredSessions();

            assertThat(count).isEqualTo(1);
            verify(sessionRepository, times(1)).save(any(McpSession.class));

            ArgumentCaptor<McpSession> captor = ArgumentCaptor.forClass(McpSession.class);
            verify(sessionRepository).save(captor.capture());
            McpSession timedOut = captor.getValue();
            assertThat(timedOut.getStatus()).isEqualTo(SessionStatus.TIMED_OUT);
            assertThat(timedOut.getCompletedAt()).isNotNull();
            assertThat(timedOut.getErrorMessage()).contains("timed out");
        }

        @Test
        @DisplayName("returns zero when no sessions have expired")
        void timeoutExpiredSessions_noExpired_returnsZero() {
            McpSession freshSession = createActiveSession();
            freshSession.setLastActivityAt(NOW);

            when(sessionRepository.findByStatus(SessionStatus.ACTIVE))
                    .thenReturn(List.of(freshSession));

            int count = service.timeoutExpiredSessions();

            assertThat(count).isEqualTo(0);
            verify(sessionRepository, never()).save(any(McpSession.class));
        }
    }

    // ── getActiveSessionCount ──

    @Nested
    @DisplayName("getActiveSessionCount")
    class GetActiveSessionCountTests {

        @Test
        @DisplayName("returns correct count from repository")
        void getActiveSessionCount_returnsCorrectCount() {
            when(sessionRepository.countByDeveloperProfileIdAndStatus(
                    PROFILE_ID, SessionStatus.ACTIVE)).thenReturn(3L);

            long count = service.getActiveSessionCount(PROFILE_ID);

            assertThat(count).isEqualTo(3L);
            verify(sessionRepository).countByDeveloperProfileIdAndStatus(
                    PROFILE_ID, SessionStatus.ACTIVE);
        }
    }
}
