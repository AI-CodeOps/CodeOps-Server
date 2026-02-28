package com.codeops.mcp.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Complete context payload delivered to AI agents on session initialization.
 *
 * <p>Assembled from all CodeOps modules in a single response, this payload
 * provides everything an AI agent needs to begin working on a project:
 * session identity, persona/directives, project documents, ecosystem context,
 * secret references, recent session history, team discussion, container
 * status, and available MCP tools.</p>
 *
 * @param sessionId        the initialized session ID
 * @param projectName      name of the target project
 * @param environment      deployment environment name
 * @param persona          assembled persona system prompt
 * @param directives       active directives for the project
 * @param documents        document type to content map
 * @param registryContext  ecosystem context from Registry module
 * @param secretReferences secret key references from Vault (references only, not values)
 * @param recentSessions   recent session history for context
 * @param teamDiscussion   recent Relay messages for team context
 * @param fleetStatus      container status from Fleet module
 * @param availableTools   available MCP tools for this session
 */
public record SessionContextPayload(
        UUID sessionId,
        String projectName,
        String environment,
        String persona,
        List<DirectiveContent> directives,
        Map<String, String> documents,
        RegistryContext registryContext,
        List<SecretReference> secretReferences,
        List<SessionHistoryEntry> recentSessions,
        List<RecentMessage> teamDiscussion,
        FleetStatusSummary fleetStatus,
        List<McpToolDefinitionResponse> availableTools
) {

    /**
     * Active directive content for the project.
     *
     * @param name     directive name
     * @param category directive category
     * @param content  directive content
     */
    public record DirectiveContent(
            String name,
            String category,
            String content
    ) {}

    /**
     * Registry ecosystem context for the target project.
     *
     * @param currentService  the registered service for this project, if any
     * @param relatedServices dependencies and dependants
     * @param allocatedPorts  ports allocated to this service
     * @param registeredRoutes API routes registered for this service
     * @param dependencies    service dependencies
     */
    public record RegistryContext(
            ServiceInfo currentService,
            List<ServiceInfo> relatedServices,
            List<PortInfo> allocatedPorts,
            List<RouteInfo> registeredRoutes,
            List<DependencyInfo> dependencies
    ) {}

    /**
     * Summary information about a registered service.
     *
     * @param id      service ID
     * @param name    service name
     * @param type    service type
     * @param status  service status
     * @param version service version
     */
    public record ServiceInfo(
            UUID id,
            String name,
            String type,
            String status,
            String version
    ) {}

    /**
     * Port allocation information.
     *
     * @param port        port number
     * @param protocol    protocol (e.g., "HTTP", "gRPC")
     * @param serviceName owning service name
     */
    public record PortInfo(
            int port,
            String protocol,
            String serviceName
    ) {}

    /**
     * Registered API route information.
     *
     * @param path        route path
     * @param method      HTTP method
     * @param serviceName owning service name
     */
    public record RouteInfo(
            String path,
            String method,
            String serviceName
    ) {}

    /**
     * Service dependency information.
     *
     * @param serviceName dependency service name
     * @param type        dependency type
     */
    public record DependencyInfo(
            String serviceName,
            String type
    ) {}

    /**
     * Reference to a secret stored in Vault (key only, not the value).
     *
     * @param key       secret key name
     * @param vaultPath path in Vault
     */
    public record SecretReference(
            String key,
            String vaultPath
    ) {}

    /**
     * Summary of a recent session for historical context.
     *
     * @param sessionId     session ID
     * @param developerName developer display name
     * @param summary       AI-generated session summary
     * @param commitHashes  list of commit hashes
     * @param completedAt   when the session completed
     * @param filesChanged  number of files changed
     */
    public record SessionHistoryEntry(
            UUID sessionId,
            String developerName,
            String summary,
            List<String> commitHashes,
            Instant completedAt,
            int filesChanged
    ) {}

    /**
     * Recent Relay message for team context.
     *
     * @param authorName display name of the message author
     * @param content    message content
     * @param sentAt     when the message was sent
     */
    public record RecentMessage(
            String authorName,
            String content,
            Instant sentAt
    ) {}

    /**
     * Fleet container status summary for the project's environment.
     *
     * @param totalContainers     total container count
     * @param runningContainers   running container count
     * @param unhealthyContainers unhealthy container count
     * @param containers          individual container details
     */
    public record FleetStatusSummary(
            int totalContainers,
            int runningContainers,
            int unhealthyContainers,
            List<ContainerInfo> containers
    ) {}

    /**
     * Summary information about a Fleet container.
     *
     * @param name         container name
     * @param serviceName  associated service name
     * @param status       container status
     * @param healthStatus container health status
     */
    public record ContainerInfo(
            String name,
            String serviceName,
            String status,
            String healthStatus
    ) {}
}
