package com.codeops.mcp.dto.mapper;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.dto.response.*;
import com.codeops.mcp.entity.*;
import com.codeops.mcp.entity.enums.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for all MCP module MapStruct mappers.
 *
 * <p>Verifies entity-to-response mapping including boolean field name translation,
 * nested entity property extraction, null-safe relationship handling, and
 * list mapping.</p>
 */
class McpMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

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

    // ── DeveloperProfileMapper ──

    @Nested
    class DeveloperProfileMapperTests {

        private final DeveloperProfileMapper mapper = Mappers.getMapper(DeveloperProfileMapper.class);

        @Test
        void toResponse_mapsAllFields() {
            User user = createUser();
            Team team = createTeam();

            DeveloperProfile entity = DeveloperProfile.builder()
                    .displayName("Dev Override")
                    .bio("Senior developer")
                    .defaultEnvironment(Environment.PRODUCTION)
                    .preferencesJson("{\"theme\":\"dark\"}")
                    .timezone("America/Chicago")
                    .team(team)
                    .user(user)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            DeveloperProfileResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.displayName()).isEqualTo("Dev Override");
            assertThat(resp.bio()).isEqualTo("Senior developer");
            assertThat(resp.defaultEnvironment()).isEqualTo(Environment.PRODUCTION);
            assertThat(resp.timezone()).isEqualTo("America/Chicago");
            assertThat(resp.isActive()).isTrue();
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.userDisplayName()).isEqualTo("Test Developer");
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }
    }

    // ── McpApiTokenMapper ──

    @Nested
    class McpApiTokenMapperTests {

        private final McpApiTokenMapper mapper = Mappers.getMapper(McpApiTokenMapper.class);

        @Test
        void toResponse_excludesTokenHash() {
            McpApiToken entity = McpApiToken.builder()
                    .name("Claude Code Laptop")
                    .tokenHash("sha256-secret-hash")
                    .tokenPrefix("mcp_a1b2")
                    .status(TokenStatus.ACTIVE)
                    .lastUsedAt(NOW)
                    .scopesJson("[\"registry\",\"fleet\"]")
                    .developerProfile(DeveloperProfile.builder().build())
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ApiTokenResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("Claude Code Laptop");
            assertThat(resp.tokenPrefix()).isEqualTo("mcp_a1b2");
            assertThat(resp.status()).isEqualTo(TokenStatus.ACTIVE);
            assertThat(resp.lastUsedAt()).isEqualTo(NOW);
            assertThat(resp.scopesJson()).isEqualTo("[\"registry\",\"fleet\"]");
            // tokenHash is NOT in the response DTO at all
        }
    }

    // ── McpSessionMapper ──

    @Nested
    class McpSessionMapperTests {

        private final McpSessionMapper mapper = Mappers.getMapper(McpSessionMapper.class);

        @Test
        void toResponse_mapsDenormalizedNames() {
            User user = createUser();
            DeveloperProfile profile = DeveloperProfile.builder()
                    .user(user)
                    .team(createTeam())
                    .build();

            McpSession entity = McpSession.builder()
                    .status(SessionStatus.ACTIVE)
                    .environment(Environment.DEVELOPMENT)
                    .transport(McpTransport.SSE)
                    .startedAt(NOW)
                    .totalToolCalls(5)
                    .developerProfile(profile)
                    .project(createProject())
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            McpSessionResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.status()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(resp.projectName()).isEqualTo("Test Project");
            assertThat(resp.developerName()).isEqualTo("Test Developer");
            assertThat(resp.environment()).isEqualTo(Environment.DEVELOPMENT);
            assertThat(resp.transport()).isEqualTo(McpTransport.SSE);
            assertThat(resp.totalToolCalls()).isEqualTo(5);
        }

        @Test
        void toDetailResponse_includesToolCallsAndResult() {
            User user = createUser();
            DeveloperProfile profile = DeveloperProfile.builder()
                    .user(user)
                    .team(createTeam())
                    .build();

            McpSession entity = McpSession.builder()
                    .status(SessionStatus.COMPLETED)
                    .environment(Environment.STAGING)
                    .transport(McpTransport.HTTP)
                    .timeoutMinutes(60)
                    .totalToolCalls(3)
                    .errorMessage(null)
                    .developerProfile(profile)
                    .project(createProject())
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            List<SessionToolCallResponse> toolCalls = List.of(
                    new SessionToolCallResponse(UUID.randomUUID(), "registry.list", "registry",
                            null, null, ToolCallStatus.SUCCESS, 100, null, NOW, NOW)
            );
            SessionResultResponse result = new SessionResultResponse(
                    UUID.randomUUID(), "Auth module done", null, null, null,
                    5, 92.5, 200, 50, null, 45, 150000L, NOW
            );

            McpSessionDetailResponse resp = mapper.toDetailResponse(entity, toolCalls, result);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.projectName()).isEqualTo("Test Project");
            assertThat(resp.developerName()).isEqualTo("Test Developer");
            assertThat(resp.toolCalls()).hasSize(1);
            assertThat(resp.result()).isNotNull();
            assertThat(resp.result().summary()).isEqualTo("Auth module done");
        }
    }

    // ── SessionToolCallMapper ──

    @Nested
    class SessionToolCallMapperTests {

        private final SessionToolCallMapper mapper = Mappers.getMapper(SessionToolCallMapper.class);

        @Test
        void toResponse_mapsAllFields() {
            SessionToolCall entity = SessionToolCall.builder()
                    .toolName("registry.listServices")
                    .toolCategory("registry")
                    .requestJson("{\"teamId\":\"123\"}")
                    .responseJson("{\"services\":[]}")
                    .status(ToolCallStatus.SUCCESS)
                    .durationMs(150)
                    .calledAt(NOW)
                    .session(McpSession.builder()
                            .environment(Environment.DEVELOPMENT)
                            .transport(McpTransport.SSE)
                            .developerProfile(DeveloperProfile.builder().build())
                            .project(createProject())
                            .build())
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            SessionToolCallResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.toolName()).isEqualTo("registry.listServices");
            assertThat(resp.toolCategory()).isEqualTo("registry");
            assertThat(resp.requestJson()).isEqualTo("{\"teamId\":\"123\"}");
            assertThat(resp.status()).isEqualTo(ToolCallStatus.SUCCESS);
            assertThat(resp.durationMs()).isEqualTo(150);
        }
    }

    // ── SessionResultMapper ──

    @Nested
    class SessionResultMapperTests {

        private final SessionResultMapper mapper = Mappers.getMapper(SessionResultMapper.class);

        @Test
        void toResponse_mapsJsonFields() {
            SessionResult entity = SessionResult.builder()
                    .summary("Implemented authentication")
                    .commitHashesJson("[\"abc123\"]")
                    .filesChangedJson("{\"created\":[\"Auth.java\"]}")
                    .endpointsChangedJson("{\"added\":[\"/api/auth\"]}")
                    .testsAdded(5)
                    .testCoverage(92.5)
                    .linesAdded(200)
                    .linesRemoved(50)
                    .durationMinutes(45)
                    .tokenUsage(150000L)
                    .session(McpSession.builder()
                            .environment(Environment.DEVELOPMENT)
                            .transport(McpTransport.SSE)
                            .developerProfile(DeveloperProfile.builder().build())
                            .project(createProject())
                            .build())
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            SessionResultResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.summary()).isEqualTo("Implemented authentication");
            assertThat(resp.commitHashesJson()).isEqualTo("[\"abc123\"]");
            assertThat(resp.testsAdded()).isEqualTo(5);
            assertThat(resp.testCoverage()).isEqualTo(92.5);
            assertThat(resp.tokenUsage()).isEqualTo(150000L);
        }
    }

    // ── ProjectDocumentMapper ──

    @Nested
    class ProjectDocumentMapperTests {

        private final ProjectDocumentMapper mapper = Mappers.getMapper(ProjectDocumentMapper.class);

        @Test
        void toResponse_mapsBasicFields() {
            User user = createUser();
            Project project = createProject();

            ProjectDocument entity = ProjectDocument.builder()
                    .documentType(DocumentType.CLAUDE_MD)
                    .lastAuthorType(AuthorType.HUMAN)
                    .lastSessionId(SESSION_ID)
                    .project(project)
                    .lastUpdatedBy(user)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            ProjectDocumentResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.documentType()).isEqualTo(DocumentType.CLAUDE_MD);
            assertThat(resp.lastAuthorType()).isEqualTo(AuthorType.HUMAN);
            assertThat(resp.isFlagged()).isFalse();
            assertThat(resp.projectId()).isEqualTo(PROJECT_ID);
            assertThat(resp.lastUpdatedByName()).isEqualTo("Test Developer");
        }

        @Test
        void toDetailResponse_includesVersions() {
            Project project = createProject();

            ProjectDocument entity = ProjectDocument.builder()
                    .documentType(DocumentType.ARCHITECTURE_MD)
                    .currentContent("# Architecture")
                    .project(project)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);
            entity.setUpdatedAt(NOW);

            List<ProjectDocumentVersionResponse> versions = List.of(
                    new ProjectDocumentVersionResponse(UUID.randomUUID(), 1,
                            "# Architecture v1", AuthorType.HUMAN,
                            "abc123", "Initial", "Dev", null, NOW)
            );

            ProjectDocumentDetailResponse resp = mapper.toDetailResponse(entity, versions);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.currentContent()).isEqualTo("# Architecture");
            assertThat(resp.versions()).hasSize(1);
        }
    }

    // ── ProjectDocumentVersionMapper ──

    @Nested
    class ProjectDocumentVersionMapperTests {

        private final ProjectDocumentVersionMapper mapper = Mappers.getMapper(ProjectDocumentVersionMapper.class);

        @Test
        void toResponse_mapsVersionAndAuthor() {
            User user = createUser();
            ProjectDocument doc = ProjectDocument.builder()
                    .documentType(DocumentType.CONVENTIONS_MD)
                    .project(createProject())
                    .build();

            McpSession session = McpSession.builder()
                    .environment(Environment.DEVELOPMENT)
                    .transport(McpTransport.SSE)
                    .developerProfile(DeveloperProfile.builder().build())
                    .project(createProject())
                    .build();
            session.setId(SESSION_ID);

            ProjectDocumentVersion entity = ProjectDocumentVersion.builder()
                    .versionNumber(3)
                    .content("# Conventions v3")
                    .authorType(AuthorType.AI)
                    .commitHash("def456")
                    .changeDescription("Updated formatting rules")
                    .document(doc)
                    .author(user)
                    .session(session)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ProjectDocumentVersionResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.versionNumber()).isEqualTo(3);
            assertThat(resp.content()).isEqualTo("# Conventions v3");
            assertThat(resp.authorType()).isEqualTo(AuthorType.AI);
            assertThat(resp.commitHash()).isEqualTo("def456");
            assertThat(resp.authorName()).isEqualTo("Test Developer");
            assertThat(resp.sessionId()).isEqualTo(SESSION_ID);
        }
    }

    // ── ActivityFeedEntryMapper ──

    @Nested
    class ActivityFeedEntryMapperTests {

        private final ActivityFeedEntryMapper mapper = Mappers.getMapper(ActivityFeedEntryMapper.class);

        @Test
        void toResponse_mapsActorNameDenormalized() {
            User user = createUser();
            Project project = createProject();

            McpSession session = McpSession.builder()
                    .environment(Environment.DEVELOPMENT)
                    .transport(McpTransport.SSE)
                    .developerProfile(DeveloperProfile.builder().build())
                    .project(project)
                    .build();
            session.setId(SESSION_ID);

            ActivityFeedEntry entity = ActivityFeedEntry.builder()
                    .activityType(ActivityType.SESSION_COMPLETED)
                    .title("Session completed: Auth module")
                    .detail("Added authentication with 5 tests")
                    .sourceModule("mcp")
                    .projectName("Test Project")
                    .team(createTeam())
                    .actor(user)
                    .project(project)
                    .session(session)
                    .build();
            entity.setId(ID);
            entity.setCreatedAt(NOW);

            ActivityFeedEntryResponse resp = mapper.toResponse(entity);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.activityType()).isEqualTo(ActivityType.SESSION_COMPLETED);
            assertThat(resp.title()).isEqualTo("Session completed: Auth module");
            assertThat(resp.actorName()).isEqualTo("Test Developer");
            assertThat(resp.projectId()).isEqualTo(PROJECT_ID);
            assertThat(resp.sessionId()).isEqualTo(SESSION_ID);
            assertThat(resp.sourceModule()).isEqualTo("mcp");
        }
    }
}
