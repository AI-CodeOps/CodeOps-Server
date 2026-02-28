package com.codeops.mcp.service;

import com.codeops.dto.response.DirectiveResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.PersonaResponse;
import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.DirectiveCategory;
import com.codeops.entity.enums.Scope;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.dto.response.FleetHealthSummaryResponse;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import com.codeops.fleet.service.ContainerManagementService;
import com.codeops.fleet.service.FleetHealthService;
import com.codeops.mcp.dto.response.McpToolDefinitionResponse;
import com.codeops.mcp.dto.response.SessionContextPayload;
import com.codeops.mcp.dto.response.SessionContextPayload.*;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.ProjectDocument;
import com.codeops.mcp.entity.SessionResult;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.McpSessionRepository;
import com.codeops.mcp.repository.ProjectDocumentRepository;
import com.codeops.mcp.repository.SessionResultRepository;
import com.codeops.registry.dto.response.*;
import com.codeops.registry.entity.enums.ServiceStatus;
import com.codeops.registry.entity.enums.ServiceType;
import com.codeops.registry.service.ApiRouteService;
import com.codeops.registry.service.DependencyGraphService;
import com.codeops.registry.service.PortAllocationService;
import com.codeops.registry.service.ServiceRegistryService;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.service.MessageService;
import com.codeops.security.SecurityUtils;
import com.codeops.service.DirectiveService;
import com.codeops.service.PersonaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContextAssemblyService}.
 *
 * <p>Verifies cross-module context assembly from Core, Registry, Relay, Fleet,
 * and Vault. Tests both happy paths and graceful degradation when individual
 * modules are unavailable.</p>
 */
@ExtendWith(MockitoExtension.class)
class ContextAssemblyServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock private PersonaService personaService;
    @Mock private DirectiveService directiveService;
    @Mock private ServiceRegistryService serviceRegistryService;
    @Mock private DependencyGraphService dependencyGraphService;
    @Mock private PortAllocationService portAllocationService;
    @Mock private ApiRouteService apiRouteService;
    @Mock private ChannelRepository channelRepository;
    @Mock private MessageService messageService;
    @Mock private FleetHealthService fleetHealthService;
    @Mock private ContainerManagementService containerManagementService;
    @Mock private ProjectDocumentRepository documentRepository;
    @Mock private McpSessionRepository sessionRepository;
    @Mock private SessionResultRepository resultRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private ContextAssemblyService service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

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
                .name("CodeOps Server")
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

    private McpSession createSession() {
        McpSession session = McpSession.builder()
                .status(SessionStatus.ACTIVE)
                .environment(Environment.DEVELOPMENT)
                .transport(McpTransport.SSE)
                .developerProfile(createProfile())
                .project(createProject())
                .build();
        session.setId(SESSION_ID);
        return session;
    }

    // ── assembleContext ──

    @Nested
    @DisplayName("assembleContext")
    class AssembleContextTests {

        @Test
        @DisplayName("assembles full payload when all modules available")
        void assembleContext_fullPayload_allModulesAvailable() {
            McpSession session = createSession();
            DeveloperProfile profile = createProfile();

            // Persona
            when(personaService.getDefaultPersona(TEAM_ID, AgentType.CODE_QUALITY))
                    .thenReturn(new PersonaResponse(UUID.randomUUID(), "Team Persona",
                            AgentType.CODE_QUALITY, "desc", "# Team Persona Content",
                            Scope.TEAM, TEAM_ID, USER_ID, "Dev", true, 1, NOW, NOW));

            // Directives
            when(directiveService.getEnabledDirectivesForProject(PROJECT_ID))
                    .thenReturn(List.of(new DirectiveResponse(UUID.randomUUID(), "Code Style",
                            "desc", "Use 4 spaces", DirectiveCategory.CONVENTIONS,
                            null, TEAM_ID, PROJECT_ID, USER_ID, "Dev", 1, NOW, NOW)));

            // Documents
            when(documentRepository.findByProjectId(PROJECT_ID))
                    .thenReturn(List.of());

            // Registry - return empty page (no matching service)
            when(serviceRegistryService.getServicesForTeam(eq(TEAM_ID), any(), any(), any(), any()))
                    .thenReturn(new com.codeops.registry.dto.response.PageResponse<>(
                            List.of(), 0, 100, 0, 0, true));

            // Sessions
            when(sessionRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(
                    PROJECT_ID, SessionStatus.COMPLETED)).thenReturn(List.of());

            // Relay - no project channel
            when(channelRepository.findByTeamIdAndProjectId(TEAM_ID, PROJECT_ID))
                    .thenReturn(Optional.empty());

            // Fleet
            when(fleetHealthService.getFleetHealthSummary(TEAM_ID))
                    .thenReturn(new FleetHealthSummaryResponse(2, 2, 0, 0, 0, 15.0, 0L, 0L, NOW));
            when(containerManagementService.listContainers(TEAM_ID))
                    .thenReturn(List.of());

            // Vault - down
            when(restTemplate.getForObject(anyString(), eq(List.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            // Session lookup for findProjectName
            when(sessionRepository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(session)));

            SessionContextPayload payload = service.assembleContext(session, profile);

            assertThat(payload).isNotNull();
            assertThat(payload.sessionId()).isEqualTo(SESSION_ID);
            assertThat(payload.projectName()).isEqualTo("CodeOps Server");
            assertThat(payload.environment()).isEqualTo("DEVELOPMENT");
            assertThat(payload.persona()).isEqualTo("# Team Persona Content");
            assertThat(payload.directives()).hasSize(1);
            assertThat(payload.availableTools()).isNotEmpty();
        }

        @Test
        @DisplayName("returns empty secrets when Vault is down")
        void assembleContext_vaultDown_returnsEmptySecrets() {
            when(restTemplate.getForObject(anyString(), eq(List.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            List<SecretReference> secrets = service.assembleSecretReferences("myservice", "DEVELOPMENT");

            assertThat(secrets).isEmpty();
        }

        @Test
        @DisplayName("returns null currentService when no Registry match")
        void assembleContext_noRegistryService_returnsNullCurrentService() {
            when(serviceRegistryService.getServicesForTeam(eq(TEAM_ID), any(), any(), any(), any()))
                    .thenReturn(new com.codeops.registry.dto.response.PageResponse<>(
                            List.of(), 0, 100, 0, 0, true));
            when(sessionRepository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(createSession())));

            RegistryContext context = service.assembleRegistryContext(PROJECT_ID, TEAM_ID);

            assertThat(context).isNotNull();
            assertThat(context.currentService()).isNull();
            assertThat(context.allocatedPorts()).isEmpty();
        }

        @Test
        @DisplayName("returns empty discussion when no project channel exists")
        void assembleContext_noRelayChannel_returnsEmptyDiscussion() {
            when(channelRepository.findByTeamIdAndProjectId(TEAM_ID, PROJECT_ID))
                    .thenReturn(Optional.empty());

            List<RecentMessage> messages = service.assembleTeamDiscussion(PROJECT_ID, TEAM_ID);

            assertThat(messages).isEmpty();
        }

        @Test
        @DisplayName("returns empty fleet when no containers")
        void assembleContext_noFleetContainers_returnsEmptyFleet() {
            when(fleetHealthService.getFleetHealthSummary(TEAM_ID))
                    .thenReturn(new FleetHealthSummaryResponse(0, 0, 0, 0, 0, 0.0, 0L, 0L, NOW));
            when(containerManagementService.listContainers(TEAM_ID))
                    .thenReturn(List.of());

            FleetStatusSummary fleet = service.assembleFleetStatus(TEAM_ID);

            assertThat(fleet).isNotNull();
            assertThat(fleet.totalContainers()).isEqualTo(0);
            assertThat(fleet.containers()).isEmpty();
        }
    }

    // ── assemblePersona ──

    @Nested
    @DisplayName("assemblePersona")
    class AssemblePersonaTests {

        @Test
        @DisplayName("returns team persona when available")
        void assemblePersona_returnsTeamPersona() {
            when(personaService.getDefaultPersona(TEAM_ID, AgentType.CODE_QUALITY))
                    .thenReturn(new PersonaResponse(UUID.randomUUID(), "Team Persona",
                            AgentType.CODE_QUALITY, "desc", "# Team Persona",
                            Scope.TEAM, TEAM_ID, USER_ID, "Dev", true, 1, NOW, NOW));

            String persona = service.assemblePersona(TEAM_ID, "CODE_QUALITY");

            assertThat(persona).isEqualTo("# Team Persona");
        }

        @Test
        @DisplayName("falls back to system persona when no team default")
        void assemblePersona_fallsBackToSystem() {
            when(personaService.getDefaultPersona(TEAM_ID, AgentType.CODE_QUALITY))
                    .thenThrow(new RuntimeException("Not found"));
            when(personaService.getSystemPersonas())
                    .thenReturn(List.of(new PersonaResponse(UUID.randomUUID(), "System QA",
                            AgentType.CODE_QUALITY, "desc", "# System Persona",
                            Scope.SYSTEM, null, USER_ID, "System", true, 1, NOW, NOW)));

            String persona = service.assemblePersona(TEAM_ID, "CODE_QUALITY");

            assertThat(persona).isEqualTo("# System Persona");
        }
    }

    // ── assembleDirectives ──

    @Nested
    @DisplayName("assembleDirectives")
    class AssembleDirectivesTests {

        @Test
        @DisplayName("returns enabled directives only")
        void assembleDirectives_returnsEnabledOnly() {
            when(directiveService.getEnabledDirectivesForProject(PROJECT_ID))
                    .thenReturn(List.of(
                            new DirectiveResponse(UUID.randomUUID(), "Code Style", "desc",
                                    "Use spaces", DirectiveCategory.CONVENTIONS,
                                    null, TEAM_ID, PROJECT_ID, USER_ID, "Dev", 1, NOW, NOW),
                            new DirectiveResponse(UUID.randomUUID(), "Architecture", "desc",
                                    "Use layers", DirectiveCategory.ARCHITECTURE,
                                    null, TEAM_ID, PROJECT_ID, USER_ID, "Dev", 1, NOW, NOW)));

            List<DirectiveContent> directives = service.assembleDirectives(PROJECT_ID);

            assertThat(directives).hasSize(2);
            assertThat(directives.get(0).name()).isEqualTo("Code Style");
            assertThat(directives.get(0).category()).isEqualTo("CONVENTIONS");
            assertThat(directives.get(1).content()).isEqualTo("Use layers");
        }
    }

    // ── assembleDocuments ──

    @Nested
    @DisplayName("assembleDocuments")
    class AssembleDocumentsTests {

        @Test
        @DisplayName("returns all documents as map")
        void assembleDocuments_returnsAllDocs() {
            ProjectDocument claudeMd = ProjectDocument.builder()
                    .documentType(DocumentType.CLAUDE_MD)
                    .currentContent("# Project Instructions")
                    .project(createProject())
                    .build();
            ProjectDocument archDoc = ProjectDocument.builder()
                    .documentType(DocumentType.ARCHITECTURE_MD)
                    .currentContent("# Architecture")
                    .project(createProject())
                    .build();

            when(documentRepository.findByProjectId(PROJECT_ID))
                    .thenReturn(List.of(claudeMd, archDoc));

            Map<String, String> docs = service.assembleDocuments(PROJECT_ID);

            assertThat(docs).hasSize(2);
            assertThat(docs).containsKey("CLAUDE_MD");
            assertThat(docs.get("CLAUDE_MD")).isEqualTo("# Project Instructions");
            assertThat(docs).containsKey("ARCHITECTURE_MD");
        }

        @Test
        @DisplayName("returns empty map when no documents exist")
        void assembleDocuments_noDocuments_returnsEmptyMap() {
            when(documentRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());

            Map<String, String> docs = service.assembleDocuments(PROJECT_ID);

            assertThat(docs).isEmpty();
        }
    }

    // ── assembleRegistryContext ──

    @Nested
    @DisplayName("assembleRegistryContext")
    class AssembleRegistryContextTests {

        @Test
        @DisplayName("returns full context with dependencies")
        void assembleRegistryContext_withDependencies() {
            UUID dbServiceId = UUID.randomUUID();
            ServiceRegistrationResponse svc = new ServiceRegistrationResponse(
                    SERVICE_ID, TEAM_ID, "CodeOps Server", "codeops-server",
                    ServiceType.SPRING_BOOT_API, "desc", null, null, "main", "Java",
                    ServiceStatus.ACTIVE, null, null, null, null, null, null,
                    USER_ID, 1, 1, 0, NOW, NOW);

            when(serviceRegistryService.getServicesForTeam(eq(TEAM_ID), any(), any(), any(), any()))
                    .thenReturn(new com.codeops.registry.dto.response.PageResponse<>(
                            List.of(svc), 0, 100, 1, 1, true));

            McpSession session = createSession();
            when(sessionRepository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(session)));

            when(portAllocationService.getPortsForService(SERVICE_ID, null))
                    .thenReturn(List.of(new PortAllocationResponse(
                            UUID.randomUUID(), SERVICE_ID, "CodeOps Server", "codeops-server",
                            "DEVELOPMENT", null, 8090, "TCP", "HTTP API", true, USER_ID, NOW)));

            when(apiRouteService.getRoutesForService(SERVICE_ID))
                    .thenReturn(List.of(new ApiRouteResponse(
                            UUID.randomUUID(), SERVICE_ID, "CodeOps Server", "codeops-server",
                            null, null, "/api/v1/", "GET,POST", "DEVELOPMENT", "API", NOW)));

            DependencyNodeResponse node1 = new DependencyNodeResponse(
                    SERVICE_ID, "CodeOps Server", "codeops-server",
                    ServiceType.SPRING_BOOT_API, ServiceStatus.ACTIVE, null);
            DependencyNodeResponse node2 = new DependencyNodeResponse(
                    dbServiceId, "PostgreSQL", "postgresql",
                    ServiceType.DATABASE_SERVICE, ServiceStatus.ACTIVE, null);
            DependencyEdgeResponse edge = new DependencyEdgeResponse(
                    SERVICE_ID, dbServiceId, null, true, null);

            when(dependencyGraphService.getDependencyGraph(TEAM_ID))
                    .thenReturn(new DependencyGraphResponse(TEAM_ID, List.of(node1, node2), List.of(edge)));

            RegistryContext context = service.assembleRegistryContext(PROJECT_ID, TEAM_ID);

            assertThat(context.currentService()).isNotNull();
            assertThat(context.currentService().name()).isEqualTo("CodeOps Server");
            assertThat(context.allocatedPorts()).hasSize(1);
            assertThat(context.allocatedPorts().get(0).port()).isEqualTo(8090);
            assertThat(context.registeredRoutes()).hasSize(1);
            assertThat(context.dependencies()).hasSize(1);
            assertThat(context.dependencies().get(0).serviceName()).isEqualTo("PostgreSQL");
        }
    }

    // ── assembleSecretReferences ──

    @Nested
    @DisplayName("assembleSecretReferences")
    class AssembleSecretReferencesTests {

        @Test
        @DisplayName("returns metadata only, never secret values")
        @SuppressWarnings("unchecked")
        void assembleSecretReferences_returnsMetadataOnly() {
            List<Map<String, String>> vaultResponse = List.of(
                    Map.of("key", "DB_PASSWORD", "path", "/services/myapp/db-password"),
                    Map.of("key", "JWT_SECRET", "path", "/services/myapp/jwt-secret"));

            when(restTemplate.getForObject(anyString(), eq(List.class)))
                    .thenReturn(vaultResponse);

            List<SecretReference> refs = service.assembleSecretReferences("myapp", "DEVELOPMENT");

            assertThat(refs).hasSize(2);
            assertThat(refs.get(0).key()).isEqualTo("DB_PASSWORD");
            assertThat(refs.get(0).vaultPath()).isEqualTo("/services/myapp/db-password");
        }
    }

    // ── assembleSessionHistory ──

    @Nested
    @DisplayName("assembleSessionHistory")
    class AssembleSessionHistoryTests {

        @Test
        @DisplayName("returns limited and ordered session history")
        void assembleSessionHistory_returnsLimitedOrdered() {
            McpSession completedSession = createSession();
            completedSession.setStatus(SessionStatus.COMPLETED);
            completedSession.setCompletedAt(NOW);

            SessionResult result = SessionResult.builder()
                    .summary("Implemented auth module")
                    .commitHashesJson("[\"abc123\",\"def456\"]")
                    .linesAdded(200)
                    .linesRemoved(50)
                    .session(completedSession)
                    .build();
            result.setId(UUID.randomUUID());

            when(sessionRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(
                    PROJECT_ID, SessionStatus.COMPLETED))
                    .thenReturn(List.of(completedSession));
            when(resultRepository.findBySessionId(completedSession.getId()))
                    .thenReturn(Optional.of(result));

            List<SessionHistoryEntry> history = service.assembleSessionHistory(PROJECT_ID, 5);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).summary()).isEqualTo("Implemented auth module");
            assertThat(history.get(0).commitHashes()).containsExactly("abc123", "def456");
            assertThat(history.get(0).filesChanged()).isEqualTo(250);
        }
    }

    // ── assembleTeamDiscussion ──

    @Nested
    @DisplayName("assembleTeamDiscussion")
    class AssembleTeamDiscussionTests {

        @Test
        @DisplayName("returns recent messages from project channel")
        void assembleTeamDiscussion_returnsRecentMessages() {
            Channel channel = new Channel();
            channel.setId(CHANNEL_ID);

            when(channelRepository.findByTeamIdAndProjectId(TEAM_ID, PROJECT_ID))
                    .thenReturn(Optional.of(channel));

            MessageResponse msg = new MessageResponse(
                    UUID.randomUUID(), CHANNEL_ID, USER_ID, "Test Developer",
                    "Working on auth module", null, null, false, null,
                    false, false, null, null, List.of(), List.of(),
                    0, null, NOW, NOW);

            when(messageService.getChannelMessages(CHANNEL_ID, TEAM_ID, USER_ID, 0, 20))
                    .thenReturn(new PageResponse<>(List.of(msg), 0, 20, 1, 1, true));

            List<RecentMessage> messages = service.assembleTeamDiscussion(PROJECT_ID, TEAM_ID);

            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).authorName()).isEqualTo("Test Developer");
            assertThat(messages.get(0).content()).isEqualTo("Working on auth module");
        }
    }

    // ── assembleFleetStatus ──

    @Nested
    @DisplayName("assembleFleetStatus")
    class AssembleFleetStatusTests {

        @Test
        @DisplayName("returns summary and container details")
        void assembleFleetStatus_returnsSummaryAndContainers() {
            when(fleetHealthService.getFleetHealthSummary(TEAM_ID))
                    .thenReturn(new FleetHealthSummaryResponse(3, 2, 1, 1, 0, 25.5, 0L, 0L, NOW));

            ContainerInstanceResponse container = new ContainerInstanceResponse(
                    UUID.randomUUID(), "docker-abc", "codeops-server", "CodeOps Server",
                    "codeops-server", "latest", ContainerStatus.RUNNING,
                    HealthStatus.HEALTHY, RestartPolicy.UNLESS_STOPPED,
                    0, 5.2, 512000000L, 1024000000L, NOW, NOW);

            when(containerManagementService.listContainers(TEAM_ID))
                    .thenReturn(List.of(container));

            FleetStatusSummary fleet = service.assembleFleetStatus(TEAM_ID);

            assertThat(fleet.totalContainers()).isEqualTo(3);
            assertThat(fleet.runningContainers()).isEqualTo(2);
            assertThat(fleet.unhealthyContainers()).isEqualTo(1);
            assertThat(fleet.containers()).hasSize(1);
            assertThat(fleet.containers().get(0).name()).isEqualTo("codeops-server");
            assertThat(fleet.containers().get(0).status()).isEqualTo("RUNNING");
        }
    }

    // ── assembleToolDefinitions ──

    @Nested
    @DisplayName("assembleToolDefinitions")
    class AssembleToolDefinitionsTests {

        @Test
        @DisplayName("returns all static tool definitions")
        void assembleToolDefinitions_returnsAllTools() {
            List<McpToolDefinitionResponse> tools = service.assembleToolDefinitions();

            assertThat(tools).isNotEmpty();
            assertThat(tools).anyMatch(t -> t.name().equals("registry.listServices"));
            assertThat(tools).anyMatch(t -> t.name().equals("fleet.listContainers"));
            assertThat(tools).anyMatch(t -> t.name().equals("documents.read"));
            assertThat(tools).anyMatch(t -> t.category().equals("registry"));
            assertThat(tools).anyMatch(t -> t.category().equals("fleet"));
            assertThat(tools).anyMatch(t -> t.category().equals("relay"));
        }
    }
}
