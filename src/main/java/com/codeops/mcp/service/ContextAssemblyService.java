package com.codeops.mcp.service;

import com.codeops.dto.response.DirectiveResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.PersonaResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.dto.response.FleetHealthSummaryResponse;
import com.codeops.fleet.service.ContainerManagementService;
import com.codeops.fleet.service.FleetHealthService;
import com.codeops.mcp.dto.response.McpToolDefinitionResponse;
import com.codeops.mcp.dto.response.SessionContextPayload;
import com.codeops.mcp.dto.response.SessionContextPayload.*;
import com.codeops.mcp.entity.DeveloperProfile;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.ProjectDocument;
import com.codeops.mcp.entity.SessionResult;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.McpSessionRepository;
import com.codeops.mcp.repository.ProjectDocumentRepository;
import com.codeops.mcp.repository.SessionResultRepository;
import com.codeops.registry.dto.response.ApiRouteResponse;
import com.codeops.registry.dto.response.DependencyEdgeResponse;
import com.codeops.registry.dto.response.DependencyGraphResponse;
import com.codeops.registry.dto.response.DependencyNodeResponse;
import com.codeops.registry.dto.response.PortAllocationResponse;
import com.codeops.registry.dto.response.ServiceRegistrationResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembles the complete context payload for MCP AI sessions.
 *
 * <p>This is the heart of the MCP module — it pulls context from all CodeOps modules
 * (Core, Registry, Relay, Fleet) and external services (Vault) to build the
 * {@link SessionContextPayload} that gives AI agents full awareness of the
 * project ecosystem.</p>
 *
 * <p>Each sub-assembly is independent and error-resilient. A failure in one module
 * (e.g., Vault is down, no Registry service matches) does not block the entire
 * context assembly — the affected section returns empty/null defaults.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContextAssemblyService {

    // Core
    private final PersonaService personaService;
    private final DirectiveService directiveService;

    // Registry
    private final ServiceRegistryService serviceRegistryService;
    private final DependencyGraphService dependencyGraphService;
    private final PortAllocationService portAllocationService;
    private final ApiRouteService apiRouteService;

    // Relay
    private final ChannelRepository channelRepository;
    private final MessageService messageService;

    // Fleet
    private final FleetHealthService fleetHealthService;
    private final ContainerManagementService containerManagementService;

    // MCP (own module)
    private final ProjectDocumentRepository documentRepository;
    private final McpSessionRepository sessionRepository;
    private final SessionResultRepository resultRepository;

    // HTTP (Vault)
    private final RestTemplate restTemplate;

    /**
     * Assembles the complete context payload for an AI session.
     *
     * <p>Pulls from all CodeOps modules via direct injection. Each sub-assembly
     * is wrapped in try-catch so a failure in one module does not block the
     * entire context.</p>
     *
     * @param session the MCP session being initialized
     * @param profile the developer profile initiating the session
     * @return the assembled context payload
     */
    @Transactional(readOnly = true)
    public SessionContextPayload assembleContext(McpSession session, DeveloperProfile profile) {
        UUID projectId = session.getProject().getId();
        UUID teamId = profile.getTeam().getId();
        String projectName = session.getProject().getName();
        String environment = session.getEnvironment().name();

        String persona = safeAssemble("persona",
                () -> assemblePersona(teamId, "CODE_QUALITY"));

        List<DirectiveContent> directives = safeAssemble("directives",
                () -> assembleDirectives(projectId));

        Map<String, String> documents = safeAssemble("documents",
                () -> assembleDocuments(projectId));

        RegistryContext registryContext = safeAssemble("registry",
                () -> assembleRegistryContext(projectId, teamId));

        List<SecretReference> secretReferences = safeAssemble("secrets",
                () -> assembleSecretReferences(projectName, environment));

        List<SessionHistoryEntry> recentSessions = safeAssemble("sessionHistory",
                () -> assembleSessionHistory(projectId, 5));

        List<RecentMessage> teamDiscussion = safeAssemble("teamDiscussion",
                () -> assembleTeamDiscussion(projectId, teamId));

        FleetStatusSummary fleetStatus = safeAssemble("fleet",
                () -> assembleFleetStatus(teamId));

        List<McpToolDefinitionResponse> tools = assembleToolDefinitions();

        log.info("Assembled context for session {} (project: {}, team: {})",
                session.getId(), projectName, teamId);

        return new SessionContextPayload(
                session.getId(),
                projectName,
                environment,
                persona,
                directives != null ? directives : List.of(),
                documents != null ? documents : Map.of(),
                registryContext,
                secretReferences != null ? secretReferences : List.of(),
                recentSessions != null ? recentSessions : List.of(),
                teamDiscussion != null ? teamDiscussion : List.of(),
                fleetStatus,
                tools
        );
    }

    /**
     * Assembles the persona content for the AI session.
     *
     * <p>Looks for a team-level default persona first, then falls back
     * to the system-level default persona for the given agent type.</p>
     *
     * @param teamId    the team ID
     * @param agentType the agent type string (maps to AgentType enum)
     * @return the persona content markdown, or null if none found
     */
    public String assemblePersona(UUID teamId, String agentType) {
        AgentType type = AgentType.valueOf(agentType);

        // Try team-level default first
        try {
            PersonaResponse teamPersona = personaService.getDefaultPersona(teamId, type);
            if (teamPersona != null) {
                return teamPersona.contentMd();
            }
        } catch (Exception e) {
            log.debug("No team default persona for type {} in team {}", agentType, teamId);
        }

        // Fall back to system personas
        try {
            List<PersonaResponse> systemPersonas = personaService.getSystemPersonas();
            return systemPersonas.stream()
                    .filter(p -> p.agentType() == type && p.isDefault())
                    .map(PersonaResponse::contentMd)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to retrieve system personas", e);
            return null;
        }
    }

    /**
     * Assembles active directives for a project.
     *
     * @param projectId the project ID
     * @return list of enabled directive contents
     */
    public List<DirectiveContent> assembleDirectives(UUID projectId) {
        List<DirectiveResponse> directives = directiveService.getEnabledDirectivesForProject(projectId);
        return directives.stream()
                .map(d -> new DirectiveContent(
                        d.name(),
                        d.category() != null ? d.category().name() : null,
                        d.contentMd()))
                .toList();
    }

    /**
     * Assembles all project documents as a map of document type to content.
     *
     * @param projectId the project ID
     * @return map of document identifier to current content
     */
    public Map<String, String> assembleDocuments(UUID projectId) {
        List<ProjectDocument> documents = documentRepository.findByProjectId(projectId);
        Map<String, String> result = new LinkedHashMap<>();
        for (ProjectDocument doc : documents) {
            String key = doc.getCustomName() != null
                    ? doc.getDocumentType().name() + ":" + doc.getCustomName()
                    : doc.getDocumentType().name();
            if (doc.getCurrentContent() != null) {
                result.put(key, doc.getCurrentContent());
            }
        }
        return result;
    }

    /**
     * Assembles the ecosystem context from the Registry module.
     *
     * <p>Looks up the service registration matching the project name,
     * then retrieves ports, routes, and dependencies.</p>
     *
     * @param projectId the project ID
     * @param teamId    the team ID
     * @return the registry context, or a context with null currentService if no match
     */
    public RegistryContext assembleRegistryContext(UUID projectId, UUID teamId) {
        ServiceInfo currentService = null;
        List<ServiceInfo> relatedServices = List.of();
        List<PortInfo> ports = List.of();
        List<RouteInfo> routes = List.of();
        List<DependencyInfo> dependencies = List.of();

        try {
            // Search for service matching the project name
            var servicesPage = serviceRegistryService.getServicesForTeam(
                    teamId, null, null, null,
                    PageRequest.of(0, 100));

            // Try to find a service matching the project
            // Match by project name or repo full name
            String projectName = findProjectName(projectId);
            ServiceRegistrationResponse matchedService = null;

            if (projectName != null && servicesPage.content() != null) {
                matchedService = servicesPage.content().stream()
                        .filter(s -> s.name().equalsIgnoreCase(projectName))
                        .findFirst()
                        .orElse(null);
            }

            if (matchedService != null) {
                currentService = new ServiceInfo(
                        matchedService.id(),
                        matchedService.name(),
                        matchedService.serviceType() != null ? matchedService.serviceType().name() : null,
                        matchedService.status() != null ? matchedService.status().name() : null,
                        null);

                // Get ports for the service
                try {
                    List<PortAllocationResponse> portAllocations =
                            portAllocationService.getPortsForService(matchedService.id(), null);
                    ports = portAllocations.stream()
                            .map(p -> new PortInfo(
                                    p.portNumber(),
                                    p.protocol(),
                                    p.serviceName()))
                            .toList();
                } catch (Exception e) {
                    log.debug("Failed to get ports for service {}", matchedService.id(), e);
                }

                // Get routes for the service
                try {
                    List<ApiRouteResponse> apiRoutes =
                            apiRouteService.getRoutesForService(matchedService.id());
                    routes = apiRoutes.stream()
                            .map(r -> new RouteInfo(
                                    r.routePrefix(),
                                    r.httpMethods(),
                                    r.serviceName()))
                            .toList();
                } catch (Exception e) {
                    log.debug("Failed to get routes for service {}", matchedService.id(), e);
                }

                // Get dependencies from the dependency graph
                try {
                    DependencyGraphResponse graph = dependencyGraphService.getDependencyGraph(teamId);
                    if (graph.edges() != null) {
                        UUID serviceId = matchedService.id();
                        Map<UUID, String> nodeNames = graph.nodes() != null
                                ? graph.nodes().stream().collect(Collectors.toMap(
                                DependencyNodeResponse::serviceId,
                                DependencyNodeResponse::name))
                                : Map.of();

                        dependencies = graph.edges().stream()
                                .filter(e -> e.sourceServiceId().equals(serviceId))
                                .map(e -> new DependencyInfo(
                                        nodeNames.getOrDefault(e.targetServiceId(), "unknown"),
                                        e.dependencyType() != null ? e.dependencyType().name() : null))
                                .toList();

                        relatedServices = graph.edges().stream()
                                .filter(e -> e.sourceServiceId().equals(serviceId)
                                        || e.targetServiceId().equals(serviceId))
                                .map(e -> e.sourceServiceId().equals(serviceId)
                                        ? e.targetServiceId() : e.sourceServiceId())
                                .distinct()
                                .map(id -> graph.nodes().stream()
                                        .filter(n -> n.serviceId().equals(id))
                                        .findFirst()
                                        .map(n -> new ServiceInfo(
                                                n.serviceId(), n.name(),
                                                n.serviceType() != null ? n.serviceType().name() : null,
                                                n.status() != null ? n.status().name() : null,
                                                null))
                                        .orElse(null))
                                .filter(Objects::nonNull)
                                .toList();
                    }
                } catch (Exception e) {
                    log.debug("Failed to get dependency graph for team {}", teamId, e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to assemble registry context for project {}", projectId, e);
        }

        return new RegistryContext(currentService, relatedServices, ports, routes, dependencies);
    }

    /**
     * Assembles secret references from Vault (metadata only, never values).
     *
     * <p>Makes an HTTP call to the Vault service. Returns empty list if Vault
     * is unavailable.</p>
     *
     * @param serviceName the service name to look up secrets for
     * @param environment the target environment
     * @return list of secret references (key + vault path)
     */
    @SuppressWarnings("unchecked")
    public List<SecretReference> assembleSecretReferences(String serviceName, String environment) {
        try {
            String url = "http://localhost:8097/api/v1/secrets/metadata?pathPrefix=/services/"
                    + serviceName + "&environment=" + environment;
            List<Map<String, String>> vaultResponse = restTemplate.getForObject(url, List.class);
            if (vaultResponse != null) {
                return vaultResponse.stream()
                        .map(entry -> new SecretReference(
                                entry.get("key"),
                                entry.get("path")))
                        .toList();
            }
        } catch (Exception e) {
            log.debug("Vault service unavailable for service {}: {}", serviceName, e.getMessage());
        }
        return List.of();
    }

    /**
     * Assembles recent session history for context continuity.
     *
     * @param projectId the project ID
     * @param limit     maximum number of sessions to return
     * @return list of session history entries, most recent first
     */
    public List<SessionHistoryEntry> assembleSessionHistory(UUID projectId, int limit) {
        List<McpSession> sessions = sessionRepository
                .findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, SessionStatus.COMPLETED);

        return sessions.stream()
                .limit(limit)
                .map(s -> {
                    SessionResult result = resultRepository.findBySessionId(s.getId()).orElse(null);
                    String developerName = s.getDeveloperProfile() != null
                            && s.getDeveloperProfile().getUser() != null
                            ? s.getDeveloperProfile().getUser().getDisplayName()
                            : null;
                    return new SessionHistoryEntry(
                            s.getId(),
                            developerName,
                            result != null ? result.getSummary() : null,
                            result != null ? parseCommitHashes(result.getCommitHashesJson()) : List.of(),
                            s.getCompletedAt(),
                            result != null ? result.getLinesAdded() + result.getLinesRemoved() : 0);
                })
                .toList();
    }

    /**
     * Assembles recent team discussion from the project's Relay channel.
     *
     * @param projectId the project ID
     * @param teamId    the team ID
     * @return list of recent messages, or empty if no project channel exists
     */
    public List<RecentMessage> assembleTeamDiscussion(UUID projectId, UUID teamId) {
        Optional<Channel> channelOpt = channelRepository.findByTeamIdAndProjectId(teamId, projectId);
        if (channelOpt.isEmpty()) {
            return List.of();
        }

        Channel channel = channelOpt.get();
        UUID userId = SecurityUtils.getCurrentUserId();

        PageResponse<MessageResponse> messages = messageService.getChannelMessages(
                channel.getId(), teamId, userId, 0, 20);

        if (messages.content() == null) {
            return List.of();
        }

        return messages.content().stream()
                .map(m -> new RecentMessage(
                        m.senderDisplayName(),
                        m.content(),
                        m.createdAt()))
                .toList();
    }

    /**
     * Assembles Fleet container status for the team.
     *
     * @param teamId the team ID
     * @return fleet status summary with container details
     */
    public FleetStatusSummary assembleFleetStatus(UUID teamId) {
        FleetHealthSummaryResponse health = fleetHealthService.getFleetHealthSummary(teamId);
        List<ContainerInstanceResponse> containers = containerManagementService.listContainers(teamId);

        List<ContainerInfo> containerInfos = containers.stream()
                .map(c -> new ContainerInfo(
                        c.containerName(),
                        c.serviceName(),
                        c.status() != null ? c.status().name() : null,
                        c.healthStatus() != null ? c.healthStatus().name() : null))
                .toList();

        return new FleetStatusSummary(
                health.totalContainers(),
                health.runningContainers(),
                health.unhealthyContainers(),
                containerInfos);
    }

    /**
     * Returns the list of available MCP tool definitions.
     *
     * <p>Currently returns a static list. Will be dynamic via Registry
     * in a future iteration.</p>
     *
     * @return list of tool definitions
     */
    public List<McpToolDefinitionResponse> assembleToolDefinitions() {
        return List.of(
                new McpToolDefinitionResponse(
                        "registry.listServices", "List all registered services for the team",
                        "registry", null),
                new McpToolDefinitionResponse(
                        "registry.getService", "Get detailed service registration info",
                        "registry", null),
                new McpToolDefinitionResponse(
                        "fleet.listContainers", "List running containers for the team",
                        "fleet", null),
                new McpToolDefinitionResponse(
                        "fleet.getContainerLogs", "Get logs from a running container",
                        "fleet", null),
                new McpToolDefinitionResponse(
                        "relay.sendMessage", "Send a message to a Relay channel",
                        "relay", null),
                new McpToolDefinitionResponse(
                        "documents.read", "Read a project document (CLAUDE.md, etc.)",
                        "documents", null),
                new McpToolDefinitionResponse(
                        "documents.update", "Update a project document",
                        "documents", null),
                new McpToolDefinitionResponse(
                        "session.recordToolCall", "Record a tool call in the session",
                        "session", null)
        );
    }

    // ── Private Helpers ──

    /**
     * Wraps a sub-assembly call with error handling, returning null on failure.
     */
    private <T> T safeAssemble(String module, java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Failed to assemble {} context: {}", module, e.getMessage());
            return null;
        }
    }

    /**
     * Looks up the project name by ID from the session repository context.
     */
    private String findProjectName(UUID projectId) {
        return sessionRepository.findByProjectId(projectId, PageRequest.of(0, 1))
                .getContent().stream()
                .findFirst()
                .map(s -> s.getProject().getName())
                .orElse(null);
    }

    /**
     * Parses a JSON array of commit hashes, returning empty list on failure.
     */
    private List<String> parseCommitHashes(String commitHashesJson) {
        if (commitHashesJson == null || commitHashesJson.isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(commitHashesJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.debug("Failed to parse commit hashes JSON: {}", commitHashesJson);
            return List.of();
        }
    }
}
