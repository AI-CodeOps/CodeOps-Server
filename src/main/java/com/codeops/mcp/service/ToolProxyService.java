package com.codeops.mcp.service;

import com.codeops.config.AppConstants;
import com.codeops.courier.dto.request.ImportCollectionRequest;
import com.codeops.courier.dto.request.SendRequestProxyRequest;
import com.codeops.courier.dto.request.StartCollectionRunRequest;
import com.codeops.courier.service.CollectionRunnerService;
import com.codeops.courier.service.HistoryService;
import com.codeops.courier.service.ImportService;
import com.codeops.courier.service.RequestProxyService;
import com.codeops.exception.AuthorizationException;
import com.codeops.fleet.dto.request.StartContainerRequest;
import com.codeops.fleet.service.ContainerManagementService;
import com.codeops.fleet.service.SolutionProfileService;
import com.codeops.logger.dto.request.LogQueryRequest;
import com.codeops.logger.service.AlertService;
import com.codeops.logger.service.AnomalyDetectionService;
import com.codeops.logger.service.LogQueryService;
import com.codeops.logger.service.MetricsService;
import com.codeops.logger.service.TraceService;
import com.codeops.mcp.dto.request.ToolCallRequest;
import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.enums.ToolCallStatus;
import com.codeops.registry.entity.enums.ServiceStatus;
import com.codeops.registry.entity.enums.ServiceType;
import com.codeops.registry.service.ApiRouteService;
import com.codeops.registry.service.ConfigEngineService;
import com.codeops.registry.service.DependencyGraphService;
import com.codeops.registry.service.PortAllocationService;
import com.codeops.registry.service.ServiceRegistryService;
import com.codeops.registry.service.SolutionService;
import com.codeops.registry.service.TopologyService;
import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.service.ChannelService;
import com.codeops.relay.service.MessageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Unified tool dispatch layer for MCP tool calls.
 *
 * <p>Routes 34 MCP tool invocations from AI agents to the appropriate CodeOps
 * module services across Registry, Logger, Courier, Relay, and Fleet. Each tool
 * call is timed, its response serialized and truncated if necessary, and the
 * result recorded in the session via {@link McpSessionService}.</p>
 *
 * <p>Tools are registered at startup via {@code @PostConstruct} into a name-keyed
 * dispatch map. The core {@link #executeTool} method parses arguments, looks up
 * the handler, executes it, and records the outcome (SUCCESS, FAILURE, or
 * UNAUTHORIZED) as a {@link com.codeops.mcp.entity.SessionToolCall}.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolProxyService {

    // ── MCP Internal ──
    private final McpSessionService sessionService;

    // ── Registry Module ──
    private final ServiceRegistryService serviceRegistryService;
    private final DependencyGraphService dependencyGraphService;
    private final PortAllocationService portAllocationService;
    private final ApiRouteService apiRouteService;
    private final ConfigEngineService configEngineService;
    private final SolutionService solutionService;
    private final TopologyService topologyService;

    // ── Logger Module ──
    private final LogQueryService logQueryService;
    private final MetricsService metricsService;
    private final TraceService traceService;
    private final AlertService alertService;
    private final AnomalyDetectionService anomalyDetectionService;

    // ── Courier Module ──
    private final RequestProxyService requestProxyService;
    private final CollectionRunnerService collectionRunnerService;
    private final HistoryService historyService;
    private final ImportService importService;

    // ── Relay Module ──
    private final MessageService messageService;
    private final ChannelService channelService;

    // ── Fleet Module ──
    private final ContainerManagementService containerManagementService;
    private final SolutionProfileService solutionProfileService;

    // ── Common ──
    private final ObjectMapper objectMapper;

    private Map<String, ToolHandler> toolRegistry;

    /**
     * Functional interface for tool dispatch handlers.
     */
    @FunctionalInterface
    interface ToolHandler {
        Object execute(Map<String, Object> args, UUID teamId, UUID userId) throws Exception;
    }

    /**
     * Registers all 34 MCP tools into the dispatch map at startup.
     */
    @PostConstruct
    void registerTools() {
        toolRegistry = new LinkedHashMap<>();

        // ── Registry (11) ──
        toolRegistry.put("registry.listServices", this::handleListServices);
        toolRegistry.put("registry.getService", this::handleGetService);
        toolRegistry.put("registry.getDependencyGraph", this::handleGetDependencyGraph);
        toolRegistry.put("registry.getImpactAnalysis", this::handleGetImpactAnalysis);
        toolRegistry.put("registry.getPortMap", this::handleGetPortMap);
        toolRegistry.put("registry.getRoutes", this::handleGetRoutes);
        toolRegistry.put("registry.generateDockerCompose", this::handleGenerateDockerCompose);
        toolRegistry.put("registry.generateAppYml", this::handleGenerateAppYml);
        toolRegistry.put("registry.getTopology", this::handleGetTopology);
        toolRegistry.put("registry.getSolution", this::handleGetSolution);
        toolRegistry.put("registry.getSolutionHealth", this::handleGetSolutionHealth);

        // ── Logger (6) ──
        toolRegistry.put("logger.queryLogs", this::handleQueryLogs);
        toolRegistry.put("logger.searchLogs", this::handleSearchLogs);
        toolRegistry.put("logger.getMetrics", this::handleGetMetrics);
        toolRegistry.put("logger.getTraceFlow", this::handleGetTraceFlow);
        toolRegistry.put("logger.getAlerts", this::handleGetAlerts);
        toolRegistry.put("logger.checkAnomalies", this::handleCheckAnomalies);

        // ── Courier (5) ──
        toolRegistry.put("courier.sendRequest", this::handleSendRequest);
        toolRegistry.put("courier.runCollection", this::handleRunCollection);
        toolRegistry.put("courier.getHistory", this::handleGetHistory);
        toolRegistry.put("courier.getRunResult", this::handleGetRunResult);
        toolRegistry.put("courier.importCollection", this::handleImportCollection);

        // ── Relay (5) ──
        toolRegistry.put("relay.sendMessage", this::handleSendMessage);
        toolRegistry.put("relay.getMessages", this::handleGetMessages);
        toolRegistry.put("relay.searchMessages", this::handleSearchMessages);
        toolRegistry.put("relay.getChannels", this::handleGetChannels);
        toolRegistry.put("relay.getUnreadCounts", this::handleGetUnreadCounts);

        // ── Fleet (7) ──
        toolRegistry.put("fleet.listContainers", this::handleListContainers);
        toolRegistry.put("fleet.inspectContainer", this::handleInspectContainer);
        toolRegistry.put("fleet.getContainerLogs", this::handleGetContainerLogs);
        toolRegistry.put("fleet.startContainer", this::handleStartContainer);
        toolRegistry.put("fleet.stopContainer", this::handleStopContainer);
        toolRegistry.put("fleet.startSolution", this::handleStartSolution);
        toolRegistry.put("fleet.stopSolution", this::handleStopSolution);

        log.info("Registered {} MCP tools across 5 modules", toolRegistry.size());
    }

    // ══════════════════════════════════════════════════════════════
    //                       CORE DISPATCH
    // ══════════════════════════════════════════════════════════════

    /**
     * Executes an MCP tool call by dispatching to the appropriate module service.
     *
     * <p>Parses the request arguments, looks up the tool handler in the registry,
     * executes it with timing, serializes and truncates the response, and records
     * the tool call in the session. Returns FAILURE for unknown tools or service
     * exceptions, and UNAUTHORIZED for authorization failures.</p>
     *
     * @param sessionId the active MCP session ID
     * @param teamId    the team context for the tool call
     * @param userId    the user context for the tool call
     * @param request   the tool call request with tool name and arguments
     * @return the recorded tool call response
     */
    public SessionToolCallResponse executeTool(UUID sessionId, UUID teamId, UUID userId,
                                               ToolCallRequest request) {
        String toolName = request.toolName();

        ToolHandler handler = toolRegistry.get(toolName);
        if (handler == null) {
            log.warn("Unknown tool '{}' requested for session {}", toolName, sessionId);
            return sessionService.recordToolCall(sessionId, request, ToolCallStatus.FAILURE,
                    0, null, "Unknown tool: " + toolName);
        }

        Map<String, Object> args = parseArguments(request.argumentsJson());

        long startTime = System.currentTimeMillis();
        try {
            Object result = handler.execute(args, teamId, userId);
            long durationMs = System.currentTimeMillis() - startTime;
            String responseJson = truncateResponse(serializeJson(result));

            log.debug("Tool {} completed in {}ms for session {}", toolName, durationMs, sessionId);
            return sessionService.recordToolCall(sessionId, request, ToolCallStatus.SUCCESS,
                    durationMs, responseJson, null);
        } catch (AuthorizationException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("Tool {} unauthorized for session {}: {}", toolName, sessionId, e.getMessage());
            return sessionService.recordToolCall(sessionId, request, ToolCallStatus.UNAUTHORIZED,
                    durationMs, null, e.getMessage());
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("Tool {} failed for session {}: {}", toolName, sessionId, e.getMessage());
            return sessionService.recordToolCall(sessionId, request, ToolCallStatus.FAILURE,
                    durationMs, null, e.getMessage());
        }
    }

    /**
     * Returns the set of all registered tool names.
     *
     * <p>Used by context assembly to advertise available tools to AI agents.</p>
     *
     * @return an unmodifiable set of tool names
     */
    public Set<String> getRegisteredToolNames() {
        return Collections.unmodifiableSet(toolRegistry.keySet());
    }

    // ══════════════════════════════════════════════════════════════
    //                    REGISTRY HANDLERS (11)
    // ══════════════════════════════════════════════════════════════

    private Object handleListServices(Map<String, Object> args, UUID teamId, UUID userId) {
        ServiceStatus status = extractEnum(args, "status", ServiceStatus.class);
        ServiceType type = extractEnum(args, "type", ServiceType.class);
        String search = extractString(args, "search");
        int page = extractInt(args, "page", 0);
        int size = extractInt(args, "size", 50);
        return serviceRegistryService.getServicesForTeam(teamId, status, type, search,
                PageRequest.of(page, size));
    }

    private Object handleGetService(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID serviceId = extractUUID(args, "serviceId");
        return serviceRegistryService.getService(serviceId);
    }

    private Object handleGetDependencyGraph(Map<String, Object> args, UUID teamId, UUID userId) {
        return dependencyGraphService.getDependencyGraph(teamId);
    }

    private Object handleGetImpactAnalysis(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID serviceId = extractUUID(args, "serviceId");
        return dependencyGraphService.getImpactAnalysis(serviceId);
    }

    private Object handleGetPortMap(Map<String, Object> args, UUID teamId, UUID userId) {
        String environment = extractString(args, "environment");
        return portAllocationService.getPortMap(teamId, environment);
    }

    private Object handleGetRoutes(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID serviceId = extractUUID(args, "serviceId");
        return apiRouteService.getRoutesForService(serviceId);
    }

    private Object handleGenerateDockerCompose(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID serviceId = extractUUID(args, "serviceId");
        String environment = extractString(args, "environment");
        return configEngineService.generateDockerCompose(serviceId, environment);
    }

    private Object handleGenerateAppYml(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID serviceId = extractUUID(args, "serviceId");
        String environment = extractString(args, "environment");
        return configEngineService.generateApplicationYml(serviceId, environment);
    }

    private Object handleGetTopology(Map<String, Object> args, UUID teamId, UUID userId) {
        return topologyService.getTopology(teamId);
    }

    private Object handleGetSolution(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID solutionId = extractUUID(args, "solutionId");
        return solutionService.getSolution(solutionId);
    }

    private Object handleGetSolutionHealth(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID solutionId = extractUUID(args, "solutionId");
        return solutionService.getSolutionHealth(solutionId);
    }

    // ══════════════════════════════════════════════════════════════
    //                    LOGGER HANDLERS (6)
    // ══════════════════════════════════════════════════════════════

    private Object handleQueryLogs(Map<String, Object> args, UUID teamId, UUID userId) {
        LogQueryRequest request = objectMapper.convertValue(args, LogQueryRequest.class);
        return logQueryService.query(request, teamId, userId);
    }

    private Object handleSearchLogs(Map<String, Object> args, UUID teamId, UUID userId) {
        String searchTerm = extractString(args, "searchTerm");
        Instant startTime = extractInstant(args, "startTime");
        Instant endTime = extractInstant(args, "endTime");
        int page = extractInt(args, "page", 0);
        int size = extractInt(args, "size", 50);
        return logQueryService.search(searchTerm, teamId, startTime, endTime, page, size);
    }

    private Object handleGetMetrics(Map<String, Object> args, UUID teamId, UUID userId) {
        String serviceName = extractString(args, "serviceName");
        return metricsService.getMetricsByService(teamId, serviceName);
    }

    private Object handleGetTraceFlow(Map<String, Object> args, UUID teamId, UUID userId) {
        String correlationId = extractString(args, "correlationId");
        return traceService.getTraceFlow(correlationId);
    }

    private Object handleGetAlerts(Map<String, Object> args, UUID teamId, UUID userId) {
        int page = extractInt(args, "page", 0);
        int size = extractInt(args, "size", 50);
        return alertService.getAlertHistory(teamId, page, size);
    }

    private Object handleCheckAnomalies(Map<String, Object> args, UUID teamId, UUID userId) {
        return anomalyDetectionService.runFullCheck(teamId);
    }

    // ══════════════════════════════════════════════════════════════
    //                    COURIER HANDLERS (5)
    // ══════════════════════════════════════════════════════════════

    private Object handleSendRequest(Map<String, Object> args, UUID teamId, UUID userId) {
        SendRequestProxyRequest request = objectMapper.convertValue(args, SendRequestProxyRequest.class);
        return requestProxyService.executeRequest(request, teamId, userId);
    }

    private Object handleRunCollection(Map<String, Object> args, UUID teamId, UUID userId) {
        StartCollectionRunRequest request = objectMapper.convertValue(args, StartCollectionRunRequest.class);
        return collectionRunnerService.startRun(request, teamId, userId);
    }

    private Object handleGetHistory(Map<String, Object> args, UUID teamId, UUID userId) {
        int page = extractInt(args, "page", 0);
        int size = extractInt(args, "size", 50);
        return historyService.getHistory(teamId, page, size);
    }

    private Object handleGetRunResult(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID runResultId = extractUUID(args, "runResultId");
        return collectionRunnerService.getRunResult(runResultId, teamId);
    }

    private Object handleImportCollection(Map<String, Object> args, UUID teamId, UUID userId) {
        ImportCollectionRequest request = objectMapper.convertValue(args, ImportCollectionRequest.class);
        return importService.importCollection(teamId, userId, request);
    }

    // ══════════════════════════════════════════════════════════════
    //                    RELAY HANDLERS (5)
    // ══════════════════════════════════════════════════════════════

    private Object handleSendMessage(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID channelId = extractUUID(args, "channelId");
        SendMessageRequest request = objectMapper.convertValue(args, SendMessageRequest.class);
        return messageService.sendMessage(channelId, request, teamId, userId);
    }

    private Object handleGetMessages(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID channelId = extractUUID(args, "channelId");
        int page = extractInt(args, "page", 0);
        int size = extractInt(args, "size", 50);
        return messageService.getChannelMessages(channelId, teamId, userId, page, size);
    }

    private Object handleSearchMessages(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID channelId = extractUUID(args, "channelId");
        String query = extractString(args, "query");
        int page = extractInt(args, "page", 0);
        int size = extractInt(args, "size", 50);
        return messageService.searchMessages(channelId, query, teamId, userId, page, size);
    }

    private Object handleGetChannels(Map<String, Object> args, UUID teamId, UUID userId) {
        return channelService.getChannelsByTeam(teamId, userId);
    }

    private Object handleGetUnreadCounts(Map<String, Object> args, UUID teamId, UUID userId) {
        return messageService.getUnreadCounts(teamId, userId);
    }

    // ══════════════════════════════════════════════════════════════
    //                    FLEET HANDLERS (7)
    // ══════════════════════════════════════════════════════════════

    private Object handleListContainers(Map<String, Object> args, UUID teamId, UUID userId) {
        return containerManagementService.listContainers(teamId);
    }

    private Object handleInspectContainer(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID containerId = extractUUID(args, "containerId");
        return containerManagementService.inspectContainer(teamId, containerId);
    }

    private Object handleGetContainerLogs(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID containerId = extractUUID(args, "containerId");
        int tailLines = extractInt(args, "tailLines", 100);
        boolean timestamps = extractBoolean(args, "timestamps", true);
        return containerManagementService.getContainerLogs(teamId, containerId, tailLines, timestamps);
    }

    private Object handleStartContainer(Map<String, Object> args, UUID teamId, UUID userId) {
        StartContainerRequest request = objectMapper.convertValue(args, StartContainerRequest.class);
        return containerManagementService.startContainer(teamId, request);
    }

    private Object handleStopContainer(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID containerId = extractUUID(args, "containerId");
        int timeoutSeconds = extractInt(args, "timeoutSeconds", 10);
        return containerManagementService.stopContainer(teamId, containerId, timeoutSeconds);
    }

    private Object handleStartSolution(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID solutionProfileId = extractUUID(args, "solutionProfileId");
        solutionProfileService.startSolution(teamId, solutionProfileId);
        return Map.of("status", "ok", "message", "Solution started");
    }

    private Object handleStopSolution(Map<String, Object> args, UUID teamId, UUID userId) {
        UUID solutionProfileId = extractUUID(args, "solutionProfileId");
        solutionProfileService.stopSolution(teamId, solutionProfileId);
        return Map.of("status", "ok", "message", "Solution stopped");
    }

    // ══════════════════════════════════════════════════════════════
    //                    PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Parses a JSON argument string into a map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argumentsJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tool call arguments: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Extracts a UUID from the arguments map.
     */
    private UUID extractUUID(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) return null;
        if (val instanceof UUID uuid) return uuid;
        return UUID.fromString(val.toString());
    }

    /**
     * Extracts a String from the arguments map.
     */
    private String extractString(Map<String, Object> args, String key) {
        Object val = args.get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * Extracts an int from the arguments map with a default value.
     */
    private int extractInt(Map<String, Object> args, String key, int defaultValue) {
        Object val = args.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number number) return number.intValue();
        return Integer.parseInt(val.toString());
    }

    /**
     * Extracts a boolean from the arguments map with a default value.
     */
    private boolean extractBoolean(Map<String, Object> args, String key, boolean defaultValue) {
        Object val = args.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(val.toString());
    }

    /**
     * Extracts an Instant from the arguments map.
     */
    private Instant extractInstant(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) return null;
        if (val instanceof Instant instant) return instant;
        return Instant.parse(val.toString());
    }

    /**
     * Extracts an enum value from the arguments map, returning null if absent or invalid.
     */
    private <E extends Enum<E>> E extractEnum(Map<String, Object> args, String key, Class<E> enumClass) {
        String val = extractString(args, key);
        if (val == null) return null;
        try {
            return Enum.valueOf(enumClass, val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Serializes an object to JSON, returning null on failure.
     */
    private String serializeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize tool response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Truncates a JSON response to the maximum allowed length.
     */
    private String truncateResponse(String json) {
        if (json == null) return null;
        if (json.length() <= AppConstants.MCP_MAX_TOOL_CALL_RESPONSE_LENGTH) return json;
        return json.substring(0, AppConstants.MCP_MAX_TOOL_CALL_RESPONSE_LENGTH) + "...[TRUNCATED]";
    }
}
