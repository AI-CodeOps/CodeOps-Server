package com.codeops.mcp.repository;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.entity.*;
import com.codeops.mcp.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MCP repositories.
 *
 * <p>Verifies custom query methods, filtering, ordering,
 * pagination, and aggregate queries for all MCP repositories
 * using an in-memory H2 database.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class McpRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DeveloperProfileRepository developerProfileRepository;

    @Autowired
    private McpApiTokenRepository mcpApiTokenRepository;

    @Autowired
    private McpSessionRepository mcpSessionRepository;

    @Autowired
    private SessionToolCallRepository sessionToolCallRepository;

    @Autowired
    private SessionResultRepository sessionResultRepository;

    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;

    @Autowired
    private ProjectDocumentVersionRepository projectDocumentVersionRepository;

    @Autowired
    private ActivityFeedEntryRepository activityFeedEntryRepository;

    private User user;
    private Team team;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("mcp-repo-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Repo Test User")
                .build();
        em.persistAndFlush(user);

        team = Team.builder()
                .name("Repo Test Team " + UUID.randomUUID())
                .owner(user)
                .build();
        em.persistAndFlush(team);

        project = Project.builder()
                .name("Repo Test Project " + UUID.randomUUID())
                .team(team)
                .createdBy(user)
                .build();
        em.persistAndFlush(project);
    }

    private DeveloperProfile createProfile(Team t, User u) {
        DeveloperProfile profile = DeveloperProfile.builder()
                .team(t)
                .user(u)
                .build();
        return em.persistAndFlush(profile);
    }

    private McpSession createSession(DeveloperProfile profile, Project p, SessionStatus status) {
        McpSession session = McpSession.builder()
                .developerProfile(profile)
                .project(p)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .status(status)
                .build();
        return em.persistAndFlush(session);
    }

    // ── DeveloperProfile Repository ──

    @Test
    void developerProfile_findByTeamIdAndUserId() {
        DeveloperProfile profile = createProfile(team, user);

        Optional<DeveloperProfile> found = developerProfileRepository.findByTeamIdAndUserId(team.getId(), user.getId());
        Optional<DeveloperProfile> notFound = developerProfileRepository.findByTeamIdAndUserId(team.getId(), UUID.randomUUID());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(profile.getId());
        assertThat(notFound).isEmpty();
    }

    @Test
    void developerProfile_findByTeamId() {
        User user2 = User.builder()
                .email("mcp-repo-test2-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .displayName("Test User 2")
                .build();
        em.persistAndFlush(user2);

        createProfile(team, user);
        createProfile(team, user2);

        List<DeveloperProfile> profiles = developerProfileRepository.findByTeamId(team.getId());
        assertThat(profiles).hasSize(2);
    }

    // ── McpApiToken Repository ──

    @Test
    void mcpApiToken_findByTokenHash() {
        DeveloperProfile profile = createProfile(team, user);
        McpApiToken token = McpApiToken.builder()
                .name("Test Token")
                .tokenHash("unique-hash-" + UUID.randomUUID())
                .tokenPrefix("mcp_test")
                .developerProfile(profile)
                .build();
        em.persistAndFlush(token);

        Optional<McpApiToken> found = mcpApiTokenRepository.findByTokenHash(token.getTokenHash());
        Optional<McpApiToken> notFound = mcpApiTokenRepository.findByTokenHash("nonexistent-hash");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Token");
        assertThat(notFound).isEmpty();
    }

    @Test
    void mcpApiToken_findByDeveloperProfileIdAndStatus() {
        DeveloperProfile profile = createProfile(team, user);

        McpApiToken active = McpApiToken.builder()
                .name("Active Token")
                .tokenHash("hash-active-" + UUID.randomUUID())
                .tokenPrefix("mcp_act1")
                .status(TokenStatus.ACTIVE)
                .developerProfile(profile)
                .build();
        em.persistAndFlush(active);

        McpApiToken revoked = McpApiToken.builder()
                .name("Revoked Token")
                .tokenHash("hash-revoked-" + UUID.randomUUID())
                .tokenPrefix("mcp_rev1")
                .status(TokenStatus.REVOKED)
                .developerProfile(profile)
                .build();
        em.persistAndFlush(revoked);

        List<McpApiToken> activeTokens = mcpApiTokenRepository.findByDeveloperProfileIdAndStatus(
                profile.getId(), TokenStatus.ACTIVE);
        List<McpApiToken> revokedTokens = mcpApiTokenRepository.findByDeveloperProfileIdAndStatus(
                profile.getId(), TokenStatus.REVOKED);

        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getName()).isEqualTo("Active Token");
        assertThat(revokedTokens).hasSize(1);
        assertThat(revokedTokens.get(0).getName()).isEqualTo("Revoked Token");
    }

    // ── McpSession Repository ──

    @Test
    void mcpSession_findByProjectIdOrderByCreatedAtDesc() {
        DeveloperProfile profile = createProfile(team, user);

        McpSession s1 = createSession(profile, project, SessionStatus.COMPLETED);
        McpSession s2 = createSession(profile, project, SessionStatus.ACTIVE);

        List<McpSession> sessions = mcpSessionRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());

        assertThat(sessions).hasSize(2);
    }

    @Test
    void mcpSession_findByStatus() {
        DeveloperProfile profile = createProfile(team, user);

        createSession(profile, project, SessionStatus.ACTIVE);
        createSession(profile, project, SessionStatus.ACTIVE);
        createSession(profile, project, SessionStatus.COMPLETED);

        List<McpSession> activeSessions = mcpSessionRepository.findByStatus(SessionStatus.ACTIVE);

        assertThat(activeSessions).hasSize(2);
    }

    @Test
    void mcpSession_findByStatusAndLastActivityAtBefore() {
        DeveloperProfile profile = createProfile(team, user);
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);

        McpSession stale = McpSession.builder()
                .developerProfile(profile)
                .project(project)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .status(SessionStatus.ACTIVE)
                .lastActivityAt(twoHoursAgo)
                .build();
        em.persistAndFlush(stale);

        McpSession recent = McpSession.builder()
                .developerProfile(profile)
                .project(project)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .status(SessionStatus.ACTIVE)
                .lastActivityAt(Instant.now())
                .build();
        em.persistAndFlush(recent);

        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        List<McpSession> timedOut = mcpSessionRepository.findByStatusAndLastActivityAtBefore(
                SessionStatus.ACTIVE, cutoff);

        assertThat(timedOut).hasSize(1);
        assertThat(timedOut.get(0).getId()).isEqualTo(stale.getId());
    }

    @Test
    void mcpSession_countByDeveloperProfileIdAndStatus() {
        DeveloperProfile profile = createProfile(team, user);

        createSession(profile, project, SessionStatus.COMPLETED);
        createSession(profile, project, SessionStatus.COMPLETED);
        createSession(profile, project, SessionStatus.FAILED);

        long completedCount = mcpSessionRepository.countByDeveloperProfileIdAndStatus(
                profile.getId(), SessionStatus.COMPLETED);
        long failedCount = mcpSessionRepository.countByDeveloperProfileIdAndStatus(
                profile.getId(), SessionStatus.FAILED);

        assertThat(completedCount).isEqualTo(2);
        assertThat(failedCount).isEqualTo(1);
    }

    // ── SessionToolCall Repository ──

    @Test
    void sessionToolCall_findBySessionIdOrderByCalledAtAsc() {
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project, SessionStatus.ACTIVE);
        Instant now = Instant.now();

        SessionToolCall tc1 = SessionToolCall.builder()
                .session(session)
                .toolName("registry.listServices")
                .toolCategory("registry")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(100)
                .calledAt(now.minus(2, ChronoUnit.MINUTES))
                .build();
        em.persistAndFlush(tc1);

        SessionToolCall tc2 = SessionToolCall.builder()
                .session(session)
                .toolName("fleet.getContainer")
                .toolCategory("fleet")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(200)
                .calledAt(now.minus(1, ChronoUnit.MINUTES))
                .build();
        em.persistAndFlush(tc2);

        List<SessionToolCall> calls = sessionToolCallRepository.findBySessionIdOrderByCalledAtAsc(session.getId());

        assertThat(calls).hasSize(2);
        assertThat(calls.get(0).getToolName()).isEqualTo("registry.listServices");
        assertThat(calls.get(1).getToolName()).isEqualTo("fleet.getContainer");
    }

    @Test
    void sessionToolCall_getToolCallSummary() {
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project, SessionStatus.ACTIVE);
        Instant now = Instant.now();

        for (int i = 0; i < 3; i++) {
            SessionToolCall tc = SessionToolCall.builder()
                    .session(session)
                    .toolName("registry.listServices")
                    .toolCategory("registry")
                    .status(ToolCallStatus.SUCCESS)
                    .durationMs(100)
                    .calledAt(now.plus(i, ChronoUnit.SECONDS))
                    .build();
            em.persistAndFlush(tc);
        }

        SessionToolCall tc = SessionToolCall.builder()
                .session(session)
                .toolName("fleet.getContainer")
                .toolCategory("fleet")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(200)
                .calledAt(now.plus(5, ChronoUnit.SECONDS))
                .build();
        em.persistAndFlush(tc);

        List<Object[]> summary = sessionToolCallRepository.getToolCallSummary(session.getId());

        assertThat(summary).hasSize(2);
        assertThat(summary.get(0)[0]).isEqualTo("registry.listServices");
        assertThat((Long) summary.get(0)[1]).isEqualTo(3L);
        assertThat(summary.get(1)[0]).isEqualTo("fleet.getContainer");
        assertThat((Long) summary.get(1)[1]).isEqualTo(1L);
    }

    // ── SessionResult Repository ──

    @Test
    void sessionResult_findBySessionId() {
        DeveloperProfile profile = createProfile(team, user);
        McpSession session = createSession(profile, project, SessionStatus.COMPLETED);

        SessionResult result = SessionResult.builder()
                .session(session)
                .summary("Test summary")
                .build();
        em.persistAndFlush(result);

        Optional<SessionResult> found = sessionResultRepository.findBySessionId(session.getId());
        Optional<SessionResult> notFound = sessionResultRepository.findBySessionId(UUID.randomUUID());

        assertThat(found).isPresent();
        assertThat(found.get().getSummary()).isEqualTo("Test summary");
        assertThat(notFound).isEmpty();
    }

    // ── ProjectDocument Repository ──

    @Test
    void projectDocument_findByProjectIdAndDocumentType() {
        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.CLAUDE_MD)
                .currentContent("# Instructions")
                .build();
        em.persistAndFlush(doc);

        Optional<ProjectDocument> found = projectDocumentRepository.findByProjectIdAndDocumentType(
                project.getId(), DocumentType.CLAUDE_MD);
        Optional<ProjectDocument> notFound = projectDocumentRepository.findByProjectIdAndDocumentType(
                project.getId(), DocumentType.OPENAPI_YAML);

        assertThat(found).isPresent();
        assertThat(found.get().getCurrentContent()).isEqualTo("# Instructions");
        assertThat(notFound).isEmpty();
    }

    @Test
    void projectDocument_findByProjectIdAndIsFlaggedTrue() {
        ProjectDocument flagged = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.AUDIT_MD)
                .isFlagged(true)
                .flagReason("3 sessions since last update")
                .build();
        em.persistAndFlush(flagged);

        ProjectDocument normal = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.CLAUDE_MD)
                .build();
        em.persistAndFlush(normal);

        List<ProjectDocument> flaggedDocs = projectDocumentRepository.findByProjectIdAndIsFlaggedTrue(project.getId());

        assertThat(flaggedDocs).hasSize(1);
        assertThat(flaggedDocs.get(0).getDocumentType()).isEqualTo(DocumentType.AUDIT_MD);
        assertThat(flaggedDocs.get(0).getFlagReason()).isEqualTo("3 sessions since last update");
    }

    // ── ProjectDocumentVersion Repository ──

    @Test
    void projectDocumentVersion_findByDocumentIdAndVersionNumber() {
        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.CONVENTIONS_MD)
                .build();
        em.persistAndFlush(doc);

        ProjectDocumentVersion v1 = ProjectDocumentVersion.builder()
                .document(doc)
                .versionNumber(1)
                .content("Version 1 content")
                .authorType(AuthorType.HUMAN)
                .build();
        em.persistAndFlush(v1);

        ProjectDocumentVersion v2 = ProjectDocumentVersion.builder()
                .document(doc)
                .versionNumber(2)
                .content("Version 2 content")
                .authorType(AuthorType.AI)
                .build();
        em.persistAndFlush(v2);

        Optional<ProjectDocumentVersion> found = projectDocumentVersionRepository
                .findByDocumentIdAndVersionNumber(doc.getId(), 2);
        Optional<ProjectDocumentVersion> notFound = projectDocumentVersionRepository
                .findByDocumentIdAndVersionNumber(doc.getId(), 99);

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Version 2 content");
        assertThat(notFound).isEmpty();
    }

    @Test
    void projectDocumentVersion_findMaxVersionNumber() {
        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(DocumentType.ARCHITECTURE_MD)
                .build();
        em.persistAndFlush(doc);

        for (int i = 1; i <= 3; i++) {
            ProjectDocumentVersion v = ProjectDocumentVersion.builder()
                    .document(doc)
                    .versionNumber(i)
                    .content("Content v" + i)
                    .authorType(AuthorType.HUMAN)
                    .build();
            em.persistAndFlush(v);
        }

        Integer maxVersion = projectDocumentVersionRepository.findMaxVersionNumber(doc.getId());
        Integer noVersions = projectDocumentVersionRepository.findMaxVersionNumber(UUID.randomUUID());

        assertThat(maxVersion).isEqualTo(3);
        assertThat(noVersions).isNull();
    }

    // ── ActivityFeedEntry Repository ──

    @Test
    void activityFeed_findByTeamIdOrderByCreatedAtDesc() {
        for (int i = 0; i < 5; i++) {
            ActivityFeedEntry entry = ActivityFeedEntry.builder()
                    .team(team)
                    .activityType(ActivityType.SESSION_COMPLETED)
                    .title("Session " + i)
                    .build();
            em.persistAndFlush(entry);
        }

        Page<ActivityFeedEntry> page = activityFeedEntryRepository.findByTeamIdOrderByCreatedAtDesc(
                team.getId(), PageRequest.of(0, 3));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void activityFeed_findByTeamIdAndCreatedAtAfter() {
        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .team(team)
                .activityType(ActivityType.DOCUMENT_UPDATED)
                .title("Recent update")
                .build();
        em.persistAndFlush(entry);

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        List<ActivityFeedEntry> recent = activityFeedEntryRepository
                .findByTeamIdAndCreatedAtAfterOrderByCreatedAtDesc(team.getId(), oneHourAgo);

        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getTitle()).isEqualTo("Recent update");
    }

    @Test
    void activityFeed_findByTeamIdAndActivityType() {
        ActivityFeedEntry completed = ActivityFeedEntry.builder()
                .team(team)
                .activityType(ActivityType.SESSION_COMPLETED)
                .title("Completed session")
                .build();
        em.persistAndFlush(completed);

        ActivityFeedEntry failed = ActivityFeedEntry.builder()
                .team(team)
                .activityType(ActivityType.SESSION_FAILED)
                .title("Failed session")
                .build();
        em.persistAndFlush(failed);

        List<ActivityFeedEntry> completedEntries = activityFeedEntryRepository
                .findByTeamIdAndActivityTypeOrderByCreatedAtDesc(team.getId(), ActivityType.SESSION_COMPLETED);
        List<ActivityFeedEntry> failedEntries = activityFeedEntryRepository
                .findByTeamIdAndActivityTypeOrderByCreatedAtDesc(team.getId(), ActivityType.SESSION_FAILED);

        assertThat(completedEntries).hasSize(1);
        assertThat(completedEntries.get(0).getTitle()).isEqualTo("Completed session");
        assertThat(failedEntries).hasSize(1);
        assertThat(failedEntries.get(0).getTitle()).isEqualTo("Failed session");
    }
}
