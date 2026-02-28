package com.codeops.mcp.service;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.dto.mapper.ActivityFeedEntryMapper;
import com.codeops.mcp.dto.response.ActivityFeedEntryResponse;
import com.codeops.mcp.entity.ActivityFeedEntry;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.SessionResult;
import com.codeops.mcp.entity.enums.ActivityType;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.ActivityFeedEntryRepository;
import com.codeops.relay.dto.response.PlatformEventResponse;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.service.PlatformEventService;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.UserRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActivityFeedService}.
 *
 * <p>Verifies activity publishing for session completion/failure,
 * document updates, convention changes, impact detection, and
 * feed query methods.</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock private ActivityFeedEntryRepository feedRepository;
    @Mock private ActivityFeedEntryMapper feedMapper;
    @Mock private PlatformEventService platformEventService;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Spy private ObjectMapper objectMapper;

    @InjectMocks
    private ActivityFeedService service;

    // ── Test Data Builders ──

    private User createUser() {
        User user = User.builder()
                .displayName("Adam Allard")
                .email("adam@allard.com")
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
                .name("CodeOps Server")
                .team(createTeam())
                .createdBy(createUser())
                .build();
        project.setId(PROJECT_ID);
        return project;
    }

    private DeveloperProfile createProfile() {
        DeveloperProfile profile = DeveloperProfile.builder()
                .displayName("Adam Allard")
                .team(createTeam())
                .user(createUser())
                .build();
        profile.setId(UUID.randomUUID());
        return profile;
    }

    private McpSession createSession() {
        McpSession session = McpSession.builder()
                .status(SessionStatus.COMPLETED)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .developerProfile(createProfile())
                .project(createProject())
                .build();
        session.setId(SESSION_ID);
        return session;
    }

    private SessionResult createResult() {
        return SessionResult.builder()
                .summary("Implemented user auth module")
                .linesAdded(200)
                .linesRemoved(50)
                .testsAdded(15)
                .build();
    }

    private ActivityFeedEntryResponse createResponse(ActivityType type, String title) {
        return new ActivityFeedEntryResponse(
                UUID.randomUUID(), type, title, null,
                "mcp", SESSION_ID, "CodeOps Server", null,
                null, "Adam Allard", PROJECT_ID, SESSION_ID, NOW);
    }

    // ── publishSessionCompleted ──

    @Nested
    @DisplayName("publishSessionCompleted")
    class PublishSessionCompletedTests {

        @Test
        @DisplayName("creates entry with project and metrics in title")
        void publishSessionCompleted_createsEntry() {
            McpSession session = createSession();
            SessionResult result = createResult();

            when(feedRepository.save(any(ActivityFeedEntry.class)))
                    .thenAnswer(inv -> {
                        ActivityFeedEntry e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        return e;
                    });
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.SESSION_COMPLETED, "Adam completed session"));
            when(platformEventService.publishEvent(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(null);

            ActivityFeedEntryResponse response = service.publishSessionCompleted(session, result);

            assertThat(response).isNotNull();

            ArgumentCaptor<ActivityFeedEntry> captor = ArgumentCaptor.forClass(ActivityFeedEntry.class);
            verify(feedRepository).save(captor.capture());
            ActivityFeedEntry entry = captor.getValue();
            assertThat(entry.getActivityType()).isEqualTo(ActivityType.SESSION_COMPLETED);
            assertThat(entry.getTitle()).contains("Adam Allard");
            assertThat(entry.getTitle()).contains("CodeOps Server");
            assertThat(entry.getTitle()).contains("250 lines changed");
            assertThat(entry.getTitle()).contains("15 tests added");
            assertThat(entry.getDetail()).isEqualTo("Implemented user auth module");
            assertThat(entry.getSourceModule()).isEqualTo("mcp");
        }

        @Test
        @DisplayName("fires Relay platform event")
        void publishSessionCompleted_firesRelayEvent() {
            McpSession session = createSession();
            SessionResult result = createResult();

            when(feedRepository.save(any(ActivityFeedEntry.class)))
                    .thenAnswer(inv -> {
                        ActivityFeedEntry e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        return e;
                    });
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.SESSION_COMPLETED, "test"));

            service.publishSessionCompleted(session, result);

            verify(platformEventService).publishEvent(
                    eq(PlatformEventType.SESSION_COMPLETED),
                    eq(TEAM_ID),
                    eq(SESSION_ID),
                    eq("mcp"),
                    anyString(),
                    anyString(),
                    eq(USER_ID),
                    isNull());
        }
    }

    // ── publishSessionFailed ──

    @Nested
    @DisplayName("publishSessionFailed")
    class PublishSessionFailedTests {

        @Test
        @DisplayName("creates entry with error in detail")
        void publishSessionFailed_createsEntry() {
            McpSession session = createSession();

            when(feedRepository.save(any(ActivityFeedEntry.class)))
                    .thenAnswer(inv -> {
                        ActivityFeedEntry e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        return e;
                    });
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.SESSION_FAILED, "test"));

            service.publishSessionFailed(session, "OutOfMemoryError");

            ArgumentCaptor<ActivityFeedEntry> captor = ArgumentCaptor.forClass(ActivityFeedEntry.class);
            verify(feedRepository).save(captor.capture());
            assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.SESSION_FAILED);
            assertThat(captor.getValue().getDetail()).isEqualTo("OutOfMemoryError");
            assertThat(captor.getValue().getTitle()).contains("failed");
        }
    }

    // ── publishDocumentUpdated ──

    @Nested
    @DisplayName("publishDocumentUpdated")
    class PublishDocumentUpdatedTests {

        @Test
        @DisplayName("creates entry with document type in title")
        void publishDocumentUpdated_createsEntry() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(feedRepository.save(any(ActivityFeedEntry.class)))
                    .thenAnswer(inv -> {
                        ActivityFeedEntry e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        return e;
                    });
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.DOCUMENT_UPDATED, "test"));

            service.publishDocumentUpdated(TEAM_ID, PROJECT_ID, USER_ID, "CLAUDE_MD", "Updated instructions");

            ArgumentCaptor<ActivityFeedEntry> captor = ArgumentCaptor.forClass(ActivityFeedEntry.class);
            verify(feedRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).contains("CLAUDE_MD");
            assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.DOCUMENT_UPDATED);
        }
    }

    // ── publishConventionChanged ──

    @Nested
    @DisplayName("publishConventionChanged")
    class PublishConventionChangedTests {

        @Test
        @DisplayName("creates entry with convention name in title")
        void publishConventionChanged_createsEntry() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(feedRepository.save(any(ActivityFeedEntry.class)))
                    .thenAnswer(inv -> {
                        ActivityFeedEntry e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        return e;
                    });
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.CONVENTION_CHANGED, "test"));

            service.publishConventionChanged(TEAM_ID, USER_ID, "Code Style Guide");

            ArgumentCaptor<ActivityFeedEntry> captor = ArgumentCaptor.forClass(ActivityFeedEntry.class);
            verify(feedRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).contains("Code Style Guide");
            assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.CONVENTION_CHANGED);
        }
    }

    // ── publishImpactDetected ──

    @Nested
    @DisplayName("publishImpactDetected")
    class PublishImpactDetectedTests {

        @Test
        @DisplayName("stores impacted service IDs as JSON")
        void publishImpactDetected_createsEntry() {
            UUID svc1 = UUID.randomUUID();
            UUID svc2 = UUID.randomUUID();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(feedRepository.save(any(ActivityFeedEntry.class)))
                    .thenAnswer(inv -> {
                        ActivityFeedEntry e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        return e;
                    });
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.IMPACT_DETECTED, "test"));

            service.publishImpactDetected(TEAM_ID, PROJECT_ID,
                    "API changes may affect services", "Breaking endpoint change",
                    List.of(svc1, svc2));

            ArgumentCaptor<ActivityFeedEntry> captor = ArgumentCaptor.forClass(ActivityFeedEntry.class);
            verify(feedRepository).save(captor.capture());
            assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.IMPACT_DETECTED);
            assertThat(captor.getValue().getImpactedServiceIdsJson()).contains(svc1.toString());
            assertThat(captor.getValue().getImpactedServiceIdsJson()).contains(svc2.toString());
        }
    }

    // ── Query Methods ──

    @Nested
    @DisplayName("Feed Queries")
    class FeedQueryTests {

        @Test
        @DisplayName("getTeamFeed returns paginated results")
        void getTeamFeed_returnsPaginated() {
            ActivityFeedEntry entry = ActivityFeedEntry.builder()
                    .activityType(ActivityType.SESSION_COMPLETED)
                    .title("test")
                    .build();
            entry.setId(UUID.randomUUID());
            Pageable pageable = PageRequest.of(0, 10);

            when(feedRepository.findByTeamIdOrderByCreatedAtDesc(TEAM_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(entry)));
            when(feedMapper.toResponse(any(ActivityFeedEntry.class)))
                    .thenReturn(createResponse(ActivityType.SESSION_COMPLETED, "test"));

            var result = service.getTeamFeed(TEAM_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("getProjectFeed returns paginated results filtered by project")
        void getProjectFeed_returnsPaginated() {
            Pageable pageable = PageRequest.of(0, 10);

            when(feedRepository.findByProjectIdOrderByCreatedAtDesc(PROJECT_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            var result = service.getProjectFeed(PROJECT_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(feedRepository).findByProjectIdOrderByCreatedAtDesc(PROJECT_ID, pageable);
        }

        @Test
        @DisplayName("getTeamActivitySince returns entries after timestamp")
        void getTeamActivitySince_returnsAfterTimestamp() {
            Instant since = Instant.now().minusSeconds(3600);
            when(feedRepository.findByTeamIdAndCreatedAtAfterOrderByCreatedAtDesc(TEAM_ID, since))
                    .thenReturn(List.of());
            when(feedMapper.toResponseList(anyList())).thenReturn(List.of());

            List<ActivityFeedEntryResponse> result = service.getTeamActivitySince(TEAM_ID, since);

            assertThat(result).isEmpty();
            verify(feedRepository).findByTeamIdAndCreatedAtAfterOrderByCreatedAtDesc(TEAM_ID, since);
        }

        @Test
        @DisplayName("getTeamActivityByType returns filtered results")
        void getTeamActivityByType_returnsFiltered() {
            when(feedRepository.findByTeamIdAndActivityTypeOrderByCreatedAtDesc(
                    TEAM_ID, ActivityType.SESSION_COMPLETED))
                    .thenReturn(List.of());
            when(feedMapper.toResponseList(anyList())).thenReturn(List.of());

            List<ActivityFeedEntryResponse> result =
                    service.getTeamActivityByType(TEAM_ID, ActivityType.SESSION_COMPLETED);

            assertThat(result).isEmpty();
            verify(feedRepository).findByTeamIdAndActivityTypeOrderByCreatedAtDesc(
                    TEAM_ID, ActivityType.SESSION_COMPLETED);
        }
    }
}
