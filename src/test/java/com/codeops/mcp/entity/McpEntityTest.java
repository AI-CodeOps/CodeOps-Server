package com.codeops.mcp.entity;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.entity.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for MCP domain objects.
 *
 * <p>Verifies persistence, default field values, builder patterns,
 * and entity relationships for all MCP entities.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class McpEntityTest {

    @Autowired
    private TestEntityManager em;

    private User createUser() {
        User user = User.builder()
                .email("mcp-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        return em.persistAndFlush(user);
    }

    private Team createTeam(User user) {
        Team team = Team.builder()
                .name("MCP Team " + UUID.randomUUID())
                .owner(user)
                .build();
        return em.persistAndFlush(team);
    }

    private Project createProject(Team team, User user) {
        Project project = Project.builder()
                .name("MCP Project " + UUID.randomUUID())
                .team(team)
                .createdBy(user)
                .build();
        return em.persistAndFlush(project);
    }

    private DeveloperProfile createProfile(Team team, User user) {
        DeveloperProfile profile = DeveloperProfile.builder()
                .team(team)
                .user(user)
                .build();
        return em.persistAndFlush(profile);
    }

    private McpSession createSession(DeveloperProfile profile, Project project) {
        McpSession session = McpSession.builder()
                .developerProfile(profile)
                .project(project)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .build();
        return em.persistAndFlush(session);
    }

    @Test
    void developerProfile_builderAndRelationships() {
        User user = createUser();
        Team team = createTeam(user);

        DeveloperProfile profile = DeveloperProfile.builder()
                .team(team)
                .user(user)
                .displayName("Test Developer")
                .bio("A test developer bio")
                .defaultEnvironment(Environment.DEVELOPMENT)
                .timezone("America/Chicago")
                .preferencesJson("{\"theme\":\"dark\"}")
                .build();

        DeveloperProfile saved = em.persistAndFlush(profile);
        em.clear();

        DeveloperProfile found = em.find(DeveloperProfile.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getDisplayName()).isEqualTo("Test Developer");
        assertThat(found.getBio()).isEqualTo("A test developer bio");
        assertThat(found.getDefaultEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(found.getTimezone()).isEqualTo("America/Chicago");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void mcpApiToken_builderAndDefaults() {
        User user = createUser();
        Team team = createTeam(user);
        DeveloperProfile profile = createProfile(team, user);

        McpApiToken token = McpApiToken.builder()
                .name("Claude Code Laptop")
                .tokenHash("sha256hashvalue")
                .tokenPrefix("mcp_a1b2")
                .developerProfile(profile)
                .build();

        McpApiToken saved = em.persistAndFlush(token);
        em.clear();

        McpApiToken found = em.find(McpApiToken.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Claude Code Laptop");
        assertThat(found.getTokenHash()).isEqualTo("sha256hashvalue");
        assertThat(found.getTokenPrefix()).isEqualTo("mcp_a1b2");
        assertThat(found.getStatus()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(found.getLastUsedAt()).isNull();
        assertThat(found.getExpiresAt()).isNull();
        assertThat(found.getDeveloperProfile().getId()).isEqualTo(profile.getId());
    }

    @Test
    void mcpSession_builderAndRelationships() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);
        DeveloperProfile profile = createProfile(team, user);

        McpSession session = McpSession.builder()
                .developerProfile(profile)
                .project(project)
                .environment(Environment.STAGING)
                .transport(McpTransport.HTTP)
                .build();

        McpSession saved = em.persistAndFlush(session);
        em.clear();

        McpSession found = em.find(McpSession.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(SessionStatus.INITIALIZING);
        assertThat(found.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(found.getTransport()).isEqualTo(McpTransport.HTTP);
        assertThat(found.getTimeoutMinutes()).isEqualTo(120);
        assertThat(found.getTotalToolCalls()).isZero();
        assertThat(found.getDeveloperProfile().getId()).isEqualTo(profile.getId());
        assertThat(found.getProject().getId()).isEqualTo(project.getId());
        assertThat(found.getToolCalls()).isEmpty();
    }

    @Test
    void sessionToolCall_builderAndFields() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project);
        Instant now = Instant.now();

        SessionToolCall toolCall = SessionToolCall.builder()
                .session(session)
                .toolName("registry.listServices")
                .toolCategory("registry")
                .requestJson("{\"teamId\":\"123\"}")
                .responseJson("{\"services\":[]}")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(150)
                .calledAt(now)
                .build();

        SessionToolCall saved = em.persistAndFlush(toolCall);
        em.clear();

        SessionToolCall found = em.find(SessionToolCall.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getToolName()).isEqualTo("registry.listServices");
        assertThat(found.getToolCategory()).isEqualTo("registry");
        assertThat(found.getRequestJson()).isEqualTo("{\"teamId\":\"123\"}");
        assertThat(found.getResponseJson()).isEqualTo("{\"services\":[]}");
        assertThat(found.getStatus()).isEqualTo(ToolCallStatus.SUCCESS);
        assertThat(found.getDurationMs()).isEqualTo(150);
        assertThat(found.getCalledAt()).isEqualTo(now);
        assertThat(found.getSession().getId()).isEqualTo(session.getId());
    }

    @Test
    void sessionResult_builderAndFields() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project);

        SessionResult result = SessionResult.builder()
                .session(session)
                .summary("Implemented user authentication module")
                .commitHashesJson("[\"abc123\",\"def456\"]")
                .filesChangedJson("{\"created\":[\"Auth.java\"],\"modified\":[],\"deleted\":[]}")
                .testsAdded(5)
                .testCoverage(92.5)
                .linesAdded(200)
                .linesRemoved(50)
                .durationMinutes(45)
                .tokenUsage(150000L)
                .build();

        SessionResult saved = em.persistAndFlush(result);
        em.clear();

        SessionResult found = em.find(SessionResult.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getSummary()).isEqualTo("Implemented user authentication module");
        assertThat(found.getCommitHashesJson()).contains("abc123");
        assertThat(found.getTestsAdded()).isEqualTo(5);
        assertThat(found.getTestCoverage()).isEqualTo(92.5);
        assertThat(found.getLinesAdded()).isEqualTo(200);
        assertThat(found.getLinesRemoved()).isEqualTo(50);
        assertThat(found.getDurationMinutes()).isEqualTo(45);
        assertThat(found.getTokenUsage()).isEqualTo(150000L);
        assertThat(found.getSession().getId()).isEqualTo(session.getId());
    }

    @Test
    void projectDocument_builderAndRelationships() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);

        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.CLAUDE_MD)
                .currentContent("# Project Instructions")
                .lastAuthorType(AuthorType.HUMAN)
                .lastUpdatedBy(user)
                .build();

        ProjectDocument saved = em.persistAndFlush(doc);
        em.clear();

        ProjectDocument found = em.find(ProjectDocument.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getDocumentType()).isEqualTo(DocumentType.CLAUDE_MD);
        assertThat(found.getCurrentContent()).isEqualTo("# Project Instructions");
        assertThat(found.getLastAuthorType()).isEqualTo(AuthorType.HUMAN);
        assertThat(found.isFlagged()).isFalse();
        assertThat(found.getProject().getId()).isEqualTo(project.getId());
        assertThat(found.getLastUpdatedBy().getId()).isEqualTo(user.getId());
        assertThat(found.getVersions()).isEmpty();
    }

    @Test
    void projectDocumentVersion_builderAndFields() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);

        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.ARCHITECTURE_MD)
                .build();
        em.persistAndFlush(doc);

        ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                .document(doc)
                .versionNumber(1)
                .content("# Architecture v1")
                .authorType(AuthorType.AI)
                .commitHash("abc123def456")
                .changeDescription("Initial architecture document")
                .author(user)
                .build();

        ProjectDocumentVersion saved = em.persistAndFlush(version);
        em.clear();

        ProjectDocumentVersion found = em.find(ProjectDocumentVersion.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getVersionNumber()).isEqualTo(1);
        assertThat(found.getContent()).isEqualTo("# Architecture v1");
        assertThat(found.getAuthorType()).isEqualTo(AuthorType.AI);
        assertThat(found.getCommitHash()).isEqualTo("abc123def456");
        assertThat(found.getChangeDescription()).isEqualTo("Initial architecture document");
        assertThat(found.getDocument().getId()).isEqualTo(doc.getId());
        assertThat(found.getAuthor().getId()).isEqualTo(user.getId());
    }

    @Test
    void activityFeedEntry_builderAndRelationships() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project);

        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .team(team)
                .activityType(ActivityType.SESSION_COMPLETED)
                .title("Session completed: Auth module")
                .detail("Added authentication with 5 tests")
                .sourceModule("mcp")
                .projectName(project.getName())
                .actor(user)
                .project(project)
                .session(session)
                .build();

        ActivityFeedEntry saved = em.persistAndFlush(entry);
        em.clear();

        ActivityFeedEntry found = em.find(ActivityFeedEntry.class, saved.getId());
        assertThat(found.getId()).isNotNull();
        assertThat(found.getActivityType()).isEqualTo(ActivityType.SESSION_COMPLETED);
        assertThat(found.getTitle()).isEqualTo("Session completed: Auth module");
        assertThat(found.getDetail()).contains("5 tests");
        assertThat(found.getSourceModule()).isEqualTo("mcp");
        assertThat(found.getTeam().getId()).isEqualTo(team.getId());
        assertThat(found.getActor().getId()).isEqualTo(user.getId());
        assertThat(found.getProject().getId()).isEqualTo(project.getId());
        assertThat(found.getSession().getId()).isEqualTo(session.getId());
    }

    @Test
    void mcpSession_addToolCall_updatesCollection() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project);

        SessionToolCall toolCall = SessionToolCall.builder()
                .toolName("fleet.listContainers")
                .toolCategory("fleet")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(100)
                .calledAt(Instant.now())
                .session(session)
                .build();

        session.getToolCalls().add(toolCall);
        em.persistAndFlush(session);
        em.clear();

        McpSession found = em.find(McpSession.class, session.getId());
        assertThat(found.getToolCalls()).hasSize(1);
        assertThat(found.getToolCalls().get(0).getToolName()).isEqualTo("fleet.listContainers");
    }

    @Test
    void projectDocument_addVersion_updatesCollection() {
        User user = createUser();
        Team team = createTeam(user);
        Project project = createProject(team, user);

        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.CONVENTIONS_MD)
                .build();
        em.persistAndFlush(doc);

        ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                .document(doc)
                .versionNumber(1)
                .content("# Conventions v1")
                .authorType(AuthorType.HUMAN)
                .build();

        doc.getVersions().add(version);
        em.persistAndFlush(doc);
        em.clear();

        ProjectDocument found = em.find(ProjectDocument.class, doc.getId());
        assertThat(found.getVersions()).hasSize(1);
        assertThat(found.getVersions().get(0).getContent()).isEqualTo("# Conventions v1");
    }
}
