package com.codeops.mcp.config;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.entity.*;
import com.codeops.mcp.entity.enums.*;
import com.codeops.mcp.repository.*;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds development data for the MCP module.
 *
 * <p>Creates developer profiles, an API token, project documents, completed
 * sessions with results, tool calls, and activity feed entries. Only active
 * under the {@code dev} profile. Skips seeding if MCP data already exists.</p>
 *
 * <p>Runs after Fleet (Order=200) and Relay (Order=100) seeders to ensure
 * all core data is available.</p>
 */
@Profile("dev")
@Component
@Order(300)
@RequiredArgsConstructor
@Slf4j
public class McpDataSeeder implements CommandLineRunner {

    private final DeveloperProfileRepository developerProfileRepository;
    private final McpApiTokenRepository apiTokenRepository;
    private final McpSessionRepository sessionRepository;
    private final SessionResultRepository sessionResultRepository;
    private final SessionToolCallRepository toolCallRepository;
    private final ProjectDocumentRepository documentRepository;
    private final ProjectDocumentVersionRepository versionRepository;
    private final ActivityFeedEntryRepository activityFeedEntryRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;

    private User adam;
    private Team team;
    private Project project;

    @Override
    @Transactional
    public void run(String... args) {
        loadReferences();
        if (team == null || adam == null || project == null) {
            log.warn("MCP seeder skipped — core data not yet available");
            return;
        }

        if (developerProfileRepository.count() > 0) {
            log.info("MCP data already seeded — skipping");
            return;
        }

        DeveloperProfile profile = seedDeveloperProfile();
        seedApiToken(profile);
        McpSession session = seedSession(profile);
        seedToolCalls(session);
        seedSessionResult(session);
        seedDocuments();
        seedActivityFeed(session);

        log.info("MCP development data seeded successfully");
    }

    /**
     * Loads the seed user, team, and project references from the core DataSeeder.
     */
    private void loadReferences() {
        adam = userRepository.findByEmail("adam@allard.com").orElse(null);
        team = teamRepository.findAll().stream().findFirst().orElse(null);
        if (team != null) {
            project = projectRepository.findByTeamId(team.getId()).stream().findFirst().orElse(null);
        }
    }

    /**
     * Creates a developer profile for the seed user.
     *
     * @return the created developer profile
     */
    private DeveloperProfile seedDeveloperProfile() {
        DeveloperProfile profile = DeveloperProfile.builder()
                .displayName(adam.getDisplayName())
                .bio("Full-stack developer specializing in AI-assisted development")
                .defaultEnvironment(Environment.LOCAL)
                .preferencesJson("{\"theme\":\"dark\",\"autoCommit\":true}")
                .timezone("America/Chicago")
                .team(team)
                .user(adam)
                .build();
        profile = developerProfileRepository.save(profile);
        log.info("Seeded developer profile for '{}'", adam.getEmail());
        return profile;
    }

    /**
     * Creates a seed API token for the developer profile.
     *
     * @param profile the developer profile
     */
    private void seedApiToken(DeveloperProfile profile) {
        McpApiToken token = McpApiToken.builder()
                .name("Dev Token")
                .tokenHash("seed_token_hash_not_for_auth")
                .tokenPrefix("mcp_seed0")
                .status(TokenStatus.ACTIVE)
                .developerProfile(profile)
                .build();
        apiTokenRepository.save(token);
        log.info("Seeded API token for developer profile {}", profile.getId());
    }

    /**
     * Creates a completed MCP session with results.
     *
     * @param profile the developer profile
     * @return the created session
     */
    private McpSession seedSession(DeveloperProfile profile) {
        Instant started = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant completed = Instant.now().minus(1, ChronoUnit.HOURS);

        McpSession session = McpSession.builder()
                .status(SessionStatus.COMPLETED)
                .environment(Environment.LOCAL)
                .transport(McpTransport.HTTP)
                .timeoutMinutes(120)
                .startedAt(started)
                .completedAt(completed)
                .lastActivityAt(completed)
                .totalToolCalls(3)
                .developerProfile(profile)
                .project(project)
                .build();
        session = sessionRepository.save(session);
        log.info("Seeded completed MCP session {}", session.getId());
        return session;
    }

    /**
     * Creates tool call records for the seed session.
     *
     * @param session the MCP session
     */
    private void seedToolCalls(McpSession session) {
        SessionToolCall call1 = SessionToolCall.builder()
                .toolName("registry.listServices")
                .toolCategory("registry")
                .requestJson("{\"status\":\"ACTIVE\"}")
                .responseJson("{\"services\":[{\"name\":\"CodeOps-Server\"}]}")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(150)
                .calledAt(session.getStartedAt().plusSeconds(30))
                .session(session)
                .build();
        toolCallRepository.save(call1);

        SessionToolCall call2 = SessionToolCall.builder()
                .toolName("fleet.listContainers")
                .toolCategory("fleet")
                .requestJson("{}")
                .responseJson("{\"containers\":[]}")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(85)
                .calledAt(session.getStartedAt().plusSeconds(60))
                .session(session)
                .build();
        toolCallRepository.save(call2);

        SessionToolCall call3 = SessionToolCall.builder()
                .toolName("logger.queryLogs")
                .toolCategory("logger")
                .requestJson("{\"level\":\"ERROR\",\"size\":10}")
                .responseJson("{\"logs\":[]}")
                .status(ToolCallStatus.SUCCESS)
                .durationMs(200)
                .calledAt(session.getStartedAt().plusSeconds(90))
                .session(session)
                .build();
        toolCallRepository.save(call3);

        log.info("Seeded 3 tool calls for session {}", session.getId());
    }

    /**
     * Creates a session result for the completed seed session.
     *
     * @param session the MCP session
     */
    private void seedSessionResult(McpSession session) {
        SessionResult result = SessionResult.builder()
                .summary("Implemented MCP module controllers and seed data")
                .commitHashesJson("[\"abc123\",\"def456\"]")
                .filesChangedJson("{\"created\":[\"McpController.java\"],\"modified\":[\"pom.xml\"],\"deleted\":[]}")
                .endpointsChangedJson("{\"added\":[\"/api/v1/mcp/sessions\"],\"modified\":[],\"removed\":[]}")
                .testsAdded(12)
                .testCoverage(95.0)
                .linesAdded(450)
                .linesRemoved(20)
                .durationMinutes(60)
                .tokenUsage(50000L)
                .session(session)
                .build();
        sessionResultRepository.save(result);
        log.info("Seeded session result for session {}", session.getId());
    }

    /**
     * Creates project documents (CLAUDE.md and ARCHITECTURE.md) for the seed project.
     */
    private void seedDocuments() {
        // CLAUDE.md document
        ProjectDocument claudeDoc = ProjectDocument.builder()
                .documentType(DocumentType.CLAUDE_MD)
                .currentContent("# CodeOps Server — CLAUDE.md\n\nProject-specific AI instructions for CodeOps Server.")
                .lastAuthorType(AuthorType.HUMAN)
                .project(project)
                .lastUpdatedBy(adam)
                .build();
        claudeDoc = documentRepository.save(claudeDoc);

        ProjectDocumentVersion claudeV1 = ProjectDocumentVersion.builder()
                .versionNumber(1)
                .content(claudeDoc.getCurrentContent())
                .authorType(AuthorType.HUMAN)
                .changeDescription("Initial CLAUDE.md creation")
                .document(claudeDoc)
                .author(adam)
                .build();
        versionRepository.save(claudeV1);

        // ARCHITECTURE.md document
        ProjectDocument archDoc = ProjectDocument.builder()
                .documentType(DocumentType.ARCHITECTURE_MD)
                .currentContent("# CodeOps Server — Architecture\n\nSpring Boot 3.3 monolith with 6 modules.")
                .lastAuthorType(AuthorType.HUMAN)
                .project(project)
                .lastUpdatedBy(adam)
                .build();
        archDoc = documentRepository.save(archDoc);

        ProjectDocumentVersion archV1 = ProjectDocumentVersion.builder()
                .versionNumber(1)
                .content(archDoc.getCurrentContent())
                .authorType(AuthorType.HUMAN)
                .changeDescription("Initial architecture document")
                .document(archDoc)
                .author(adam)
                .build();
        versionRepository.save(archV1);

        log.info("Seeded 2 project documents for project '{}'", project.getName());
    }

    /**
     * Creates activity feed entries for the seed session.
     *
     * @param session the completed MCP session
     */
    private void seedActivityFeed(McpSession session) {
        ActivityFeedEntry sessionEntry = ActivityFeedEntry.builder()
                .activityType(ActivityType.SESSION_COMPLETED)
                .title(adam.getDisplayName() + " completed session on " + project.getName())
                .detail("Implemented MCP module controllers and seed data")
                .sourceModule("mcp")
                .sourceEntityId(session.getId())
                .projectName(project.getName())
                .team(team)
                .actor(adam)
                .project(project)
                .session(session)
                .build();
        activityFeedEntryRepository.save(sessionEntry);

        ActivityFeedEntry docEntry = ActivityFeedEntry.builder()
                .activityType(ActivityType.DOCUMENT_UPDATED)
                .title(adam.getDisplayName() + " updated CLAUDE_MD on " + project.getName())
                .detail("Initial CLAUDE.md creation")
                .sourceModule("mcp")
                .projectName(project.getName())
                .team(team)
                .actor(adam)
                .project(project)
                .build();
        activityFeedEntryRepository.save(docEntry);

        log.info("Seeded 2 activity feed entries");
    }
}
