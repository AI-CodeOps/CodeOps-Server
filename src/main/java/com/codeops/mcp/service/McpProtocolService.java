package com.codeops.mcp.service;

import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.dto.McpToolDefinition;
import com.codeops.mcp.dto.request.ToolCallRequest;
import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.entity.enums.ToolCallStatus;
import com.codeops.mcp.repository.McpSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core MCP protocol handler implementing JSON-RPC 2.0 message framing.
 *
 * <p>Parses incoming JSON-RPC requests from AI agents, routes them to the
 * appropriate handler based on the method name, and formats JSON-RPC responses.
 * Supports the MCP protocol methods: {@code initialize}, {@code tools/list},
 * {@code tools/call}, {@code resources/list}, and {@code resources/read}.</p>
 *
 * <p>Tool calls are dispatched to {@link ToolProxyService} which handles
 * the actual cross-module service routing. Project documents are exposed as
 * MCP resources via {@link DocumentManagementService}.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpProtocolService {

    private static final String JSONRPC_VERSION = "2.0";
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "CodeOps MCP Gateway";
    private static final String SERVER_VERSION = "1.0.0";

    // JSON-RPC 2.0 error codes
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;

    private static final String DOCUMENT_URI_PREFIX = "document:///";

    private final ToolProxyService toolProxyService;
    private final McpSessionService sessionService;
    private final DocumentManagementService documentManagementService;
    private final ContextAssemblyService contextAssemblyService;
    private final McpSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    private List<McpToolDefinition> toolDefinitions;

    /**
     * Initializes tool definitions at startup.
     */
    @PostConstruct
    void init() {
        toolDefinitions = buildToolDefinitions();
        log.info("MCP Protocol Service initialized with {} tool definitions", toolDefinitions.size());
    }

    // ══════════════════════════════════════════════════════════════
    //                    REQUEST ROUTING
    // ══════════════════════════════════════════════════════════════

    /**
     * Handles an incoming MCP JSON-RPC request.
     *
     * <p>Parses the JSON-RPC envelope, validates the version field, extracts
     * the method name, and routes to the appropriate handler. Returns a
     * JSON-RPC formatted response string.</p>
     *
     * @param jsonRpcRequest raw JSON-RPC request string
     * @param sessionContext authenticated session context (from token or JWT)
     * @return JSON-RPC response string
     */
    public String handleRequest(String jsonRpcRequest, McpSessionContext sessionContext) {
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonRpcRequest);
        } catch (Exception e) {
            log.warn("Failed to parse JSON-RPC request: {}", e.getMessage());
            return buildErrorResponse(null, PARSE_ERROR, "Parse error: " + e.getMessage());
        }

        String version = root.path("jsonrpc").asText(null);
        if (!JSONRPC_VERSION.equals(version)) {
            String id = root.path("id").asText(null);
            return buildErrorResponse(id, INVALID_REQUEST, "Invalid or missing JSON-RPC version");
        }

        String method = root.path("method").asText(null);
        String id = root.path("id").asText(null);
        JsonNode params = root.path("params");

        if (method == null || method.isBlank()) {
            return buildErrorResponse(id, INVALID_REQUEST, "Missing method");
        }

        try {
            return switch (method) {
                case "initialize" -> handleInitialize(params, id);
                case "tools/list" -> handleToolsList(params, id, sessionContext);
                case "tools/call" -> handleToolsCall(params, id, sessionContext);
                case "resources/list" -> handleResourcesList(params, id, sessionContext);
                case "resources/read" -> handleResourcesRead(params, id, sessionContext);
                default -> buildErrorResponse(id, METHOD_NOT_FOUND, "Method not found: " + method);
            };
        } catch (Exception e) {
            log.error("Internal error handling method '{}': {}", method, e.getMessage(), e);
            return buildErrorResponse(id, INTERNAL_ERROR, "Internal error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    PROTOCOL HANDLERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Handles the {@code initialize} method — returns server capabilities and info.
     *
     * @param params the request params (unused for initialize)
     * @param id     the JSON-RPC request ID
     * @return JSON-RPC response with server info, capabilities, and protocol version
     */
    String handleInitialize(JsonNode params, String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);

        Map<String, String> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        result.put("serverInfo", serverInfo);

        result.put("capabilities", buildServerCapabilities());

        log.info("MCP initialize completed — protocol version {}", PROTOCOL_VERSION);
        return buildSuccessResponse(id, result);
    }

    /**
     * Handles the {@code tools/list} method — returns all available MCP tools with schemas.
     *
     * @param params  the request params
     * @param id      the JSON-RPC request ID
     * @param context the authenticated session context
     * @return JSON-RPC response with tool definitions
     */
    String handleToolsList(JsonNode params, String id, McpSessionContext context) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (McpToolDefinition def : toolDefinitions) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", def.name());
            tool.put("description", def.description());
            tool.put("inputSchema", def.inputSchema());
            tools.add(tool);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tools", tools);

        return buildSuccessResponse(id, result);
    }

    /**
     * Handles the {@code tools/call} method — dispatches a tool call via ToolProxyService.
     *
     * <p>Converts the MCP underscore tool name to the internal dot format,
     * delegates to {@link ToolProxyService#executeTool}, and wraps the response
     * in the MCP content array format.</p>
     *
     * @param params  the request params containing tool name and arguments
     * @param id      the JSON-RPC request ID
     * @param context the authenticated session context
     * @return JSON-RPC response with tool call result in MCP content format
     */
    String handleToolsCall(JsonNode params, String id, McpSessionContext context) {
        if (params == null || params.isMissingNode()) {
            return buildErrorResponse(id, INVALID_PARAMS, "Missing params for tools/call");
        }

        String toolName = params.path("name").asText(null);
        if (toolName == null || toolName.isBlank()) {
            return buildErrorResponse(id, INVALID_PARAMS, "Missing tool name in params");
        }

        if (context.sessionId() == null) {
            return buildErrorResponse(id, INVALID_REQUEST, "No active session for tool call");
        }

        // Convert underscore format to dot format (registry_listServices → registry.listServices)
        String internalName = toolName.replace('_', '.');
        String category = internalName.contains(".")
                ? internalName.substring(0, internalName.indexOf('.'))
                : "unknown";

        JsonNode arguments = params.path("arguments");
        String argumentsJson = arguments.isMissingNode() || arguments.isNull()
                ? "{}" : arguments.toString();

        ToolCallRequest request = new ToolCallRequest(internalName, category, argumentsJson);
        SessionToolCallResponse response = toolProxyService.executeTool(
                context.sessionId(), context.teamId(), context.userId(), request);

        // Build MCP content response
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");

        if (response.status() == ToolCallStatus.SUCCESS) {
            textContent.put("text", response.responseJson() != null ? response.responseJson() : "");
        } else {
            textContent.put("text", response.errorMessage() != null
                    ? response.errorMessage() : "Tool call failed");
            result.put("isError", true);
        }

        content.add(textContent);
        result.put("content", content);

        return buildSuccessResponse(id, result);
    }

    /**
     * Handles the {@code resources/list} method — lists project documents as MCP resources.
     *
     * @param params  the request params
     * @param id      the JSON-RPC request ID
     * @param context the authenticated session context
     * @return JSON-RPC response with resource list
     */
    String handleResourcesList(JsonNode params, String id, McpSessionContext context) {
        UUID projectId = resolveProjectId(context);
        if (projectId == null) {
            return buildErrorResponse(id, INVALID_REQUEST, "No active session with project");
        }

        List<ProjectDocumentResponse> documents = documentManagementService.getProjectDocuments(projectId);

        List<Map<String, String>> resources = new ArrayList<>();
        for (ProjectDocumentResponse doc : documents) {
            Map<String, String> resource = new LinkedHashMap<>();
            String uri = buildDocumentUri(doc.documentType(), doc.customName());
            resource.put("uri", uri);
            resource.put("name", doc.documentType() == DocumentType.CUSTOM && doc.customName() != null
                    ? doc.customName()
                    : doc.documentType().name());
            resource.put("description", describeDocumentType(doc.documentType(), doc.customName()));
            resource.put("mimeType", doc.documentType() == DocumentType.OPENAPI_YAML
                    ? "application/yaml" : "text/markdown");
            resources.add(resource);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resources", resources);

        return buildSuccessResponse(id, result);
    }

    /**
     * Handles the {@code resources/read} method — reads a specific project document.
     *
     * @param params  the request params containing the resource URI
     * @param id      the JSON-RPC request ID
     * @param context the authenticated session context
     * @return JSON-RPC response with document content
     */
    String handleResourcesRead(JsonNode params, String id, McpSessionContext context) {
        if (params == null || params.isMissingNode()) {
            return buildErrorResponse(id, INVALID_PARAMS, "Missing params for resources/read");
        }

        String uri = params.path("uri").asText(null);
        if (uri == null || !uri.startsWith(DOCUMENT_URI_PREFIX)) {
            return buildErrorResponse(id, INVALID_PARAMS, "Invalid resource URI");
        }

        UUID projectId = resolveProjectId(context);
        if (projectId == null) {
            return buildErrorResponse(id, INVALID_REQUEST, "No active session with project");
        }

        String path = uri.substring(DOCUMENT_URI_PREFIX.length());
        ProjectDocumentDetailResponse document;

        try {
            if (path.startsWith("CUSTOM/")) {
                String customName = path.substring("CUSTOM/".length());
                document = documentManagementService.getCustomDocument(projectId, customName);
            } else {
                DocumentType docType = DocumentType.valueOf(path);
                document = documentManagementService.getDocument(projectId, docType);
            }
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(id, INVALID_PARAMS, "Unknown document type: " + path);
        } catch (Exception e) {
            return buildErrorResponse(id, INTERNAL_ERROR, "Failed to read resource: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> contents = new ArrayList<>();
        Map<String, String> content = new LinkedHashMap<>();
        content.put("uri", uri);
        content.put("mimeType", document.documentType() == DocumentType.OPENAPI_YAML
                ? "application/yaml" : "text/markdown");
        content.put("text", document.currentContent() != null ? document.currentContent() : "");
        contents.add(content);
        result.put("contents", contents);

        return buildSuccessResponse(id, result);
    }

    // ══════════════════════════════════════════════════════════════
    //                    BUILDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Builds the server capabilities object for the initialize response.
     *
     * @return a map representing MCP server capabilities (tools and resources)
     */
    JsonNode buildServerCapabilities() {
        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.putObject("tools");
        capabilities.putObject("resources");
        return capabilities;
    }

    /**
     * Builds tool definitions for all 34 registered MCP tools with JSON schemas.
     *
     * <p>Tool names are converted from the internal dot format to the MCP
     * underscore format (e.g., {@code registry.listServices} becomes
     * {@code registry_listServices}).</p>
     *
     * @return list of tool definitions with input schemas
     */
    List<McpToolDefinition> buildToolDefinitions() {
        List<McpToolDefinition> definitions = new ArrayList<>();

        // ── Registry (11) ──
        definitions.add(tool("registry_listServices",
                "List registered services in the team with optional filters",
                schema()
                        .optStr("status", "Filter by service status")
                        .optStr("type", "Filter by service type")
                        .optStr("search", "Search term")
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("registry_getService",
                "Get a service registration by ID",
                schema().reqStr("serviceId", "Service UUID").build()));

        definitions.add(tool("registry_getDependencyGraph",
                "Get the dependency graph for the team",
                emptySchema()));

        definitions.add(tool("registry_getImpactAnalysis",
                "Analyze impact of changes to a service",
                schema().reqStr("serviceId", "Service UUID").build()));

        definitions.add(tool("registry_getPortMap",
                "Get the port allocation map for the team",
                schema().optStr("environment", "Target environment").build()));

        definitions.add(tool("registry_getRoutes",
                "Get API routes registered for a service",
                schema().reqStr("serviceId", "Service UUID").build()));

        definitions.add(tool("registry_generateDockerCompose",
                "Generate Docker Compose configuration for a service",
                schema()
                        .reqStr("serviceId", "Service UUID")
                        .optStr("environment", "Target environment").build()));

        definitions.add(tool("registry_generateAppYml",
                "Generate Spring Boot application.yml for a service",
                schema()
                        .reqStr("serviceId", "Service UUID")
                        .optStr("environment", "Target environment").build()));

        definitions.add(tool("registry_getTopology",
                "Get the service topology for the team",
                emptySchema()));

        definitions.add(tool("registry_getSolution",
                "Get a solution (service group) by ID",
                schema().reqStr("solutionId", "Solution UUID").build()));

        definitions.add(tool("registry_getSolutionHealth",
                "Get aggregated health for a solution",
                schema().reqStr("solutionId", "Solution UUID").build()));

        // ── Logger (6) ──
        definitions.add(tool("logger_queryLogs",
                "Query log entries with structured filters",
                schema()
                        .optStr("serviceName", "Filter by service name")
                        .optStr("level", "Log level filter (ERROR, WARN, INFO, DEBUG)")
                        .optStr("startTime", "Start time (ISO-8601)")
                        .optStr("endTime", "End time (ISO-8601)")
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("logger_searchLogs",
                "Full-text search across log entries",
                schema()
                        .reqStr("searchTerm", "Search term")
                        .optStr("startTime", "Start time (ISO-8601)")
                        .optStr("endTime", "End time (ISO-8601)")
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("logger_getMetrics",
                "Get metrics for a service",
                schema().reqStr("serviceName", "Service name").build()));

        definitions.add(tool("logger_getTraceFlow",
                "Get the distributed trace flow for a correlation ID",
                schema().reqStr("correlationId", "Correlation ID").build()));

        definitions.add(tool("logger_getAlerts",
                "Get alert history for the team",
                schema()
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("logger_checkAnomalies",
                "Run anomaly detection across all baselines for the team",
                emptySchema()));

        // ── Courier (5) ──
        definitions.add(tool("courier_sendRequest",
                "Execute an HTTP request via the Courier proxy",
                schema()
                        .reqStr("url", "Target URL")
                        .reqStr("method", "HTTP method (GET, POST, PUT, DELETE, etc.)")
                        .optStr("body", "Request body")
                        .optStr("contentType", "Content-Type header").build()));

        definitions.add(tool("courier_runCollection",
                "Run an API collection test suite",
                schema().reqStr("collectionId", "Collection UUID").build()));

        definitions.add(tool("courier_getHistory",
                "Get request execution history for the team",
                schema()
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("courier_getRunResult",
                "Get the result of a collection run",
                schema().reqStr("runResultId", "Run result UUID").build()));

        definitions.add(tool("courier_importCollection",
                "Import an API collection from external format",
                schema()
                        .reqStr("format", "Import format (POSTMAN, OPENAPI, CURL)")
                        .reqStr("content", "Import content (JSON or YAML string)").build()));

        // ── Relay (5) ──
        definitions.add(tool("relay_sendMessage",
                "Send a message to a Relay channel",
                schema()
                        .reqStr("channelId", "Channel UUID")
                        .reqStr("content", "Message content").build()));

        definitions.add(tool("relay_getMessages",
                "Get messages from a Relay channel",
                schema()
                        .reqStr("channelId", "Channel UUID")
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("relay_searchMessages",
                "Search messages in a Relay channel",
                schema()
                        .reqStr("channelId", "Channel UUID")
                        .reqStr("query", "Search query")
                        .optInt("page", "Page number (default 0)")
                        .optInt("size", "Page size (default 50)").build()));

        definitions.add(tool("relay_getChannels",
                "List Relay channels for the team",
                emptySchema()));

        definitions.add(tool("relay_getUnreadCounts",
                "Get unread message counts per channel",
                emptySchema()));

        // ── Fleet (7) ──
        definitions.add(tool("fleet_listContainers",
                "List Docker containers managed by Fleet",
                emptySchema()));

        definitions.add(tool("fleet_inspectContainer",
                "Get detailed information about a container",
                schema().reqStr("containerId", "Container UUID").build()));

        definitions.add(tool("fleet_getContainerLogs",
                "Get log output from a container",
                schema()
                        .reqStr("containerId", "Container UUID")
                        .optInt("tailLines", "Number of lines from the end (default 100)")
                        .optBool("timestamps", "Include timestamps (default true)").build()));

        definitions.add(tool("fleet_startContainer",
                "Start a container from a service profile",
                schema().reqStr("serviceProfileId", "Service profile UUID").build()));

        definitions.add(tool("fleet_stopContainer",
                "Stop a running container",
                schema()
                        .reqStr("containerId", "Container UUID")
                        .optInt("timeoutSeconds", "Graceful shutdown timeout (default 10)").build()));

        definitions.add(tool("fleet_startSolution",
                "Start all containers in a solution profile",
                schema().reqStr("solutionProfileId", "Solution profile UUID").build()));

        definitions.add(tool("fleet_stopSolution",
                "Stop all containers in a solution profile",
                schema().reqStr("solutionProfileId", "Solution profile UUID").build()));

        return definitions;
    }

    /**
     * Builds a JSON-RPC 2.0 success response.
     *
     * @param id     the request ID to echo back
     * @param result the result object to include in the response
     * @return the serialized JSON-RPC response string
     */
    String buildSuccessResponse(String id, Object result) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", JSONRPC_VERSION);
            if (id != null) {
                response.put("id", id);
            } else {
                response.putNull("id");
            }
            response.set("result", objectMapper.valueToTree(result));
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to build success response: {}", e.getMessage());
            return "{\"jsonrpc\":\"2.0\",\"id\":" + (id != null ? "\"" + id + "\"" : "null")
                    + ",\"error\":{\"code\":-32603,\"message\":\"Failed to serialize response\"}}";
        }
    }

    /**
     * Builds a JSON-RPC 2.0 error response.
     *
     * @param id      the request ID to echo back (may be null)
     * @param code    the JSON-RPC error code
     * @param message the error message
     * @return the serialized JSON-RPC error response string
     */
    String buildErrorResponse(String id, int code, String message) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", JSONRPC_VERSION);
            if (id != null) {
                response.put("id", id);
            } else {
                response.putNull("id");
            }
            ObjectNode error = response.putObject("error");
            error.put("code", code);
            error.put("message", message);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to build error response: {}", e.getMessage());
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Resolves the projectId from the session in the context.
     */
    private UUID resolveProjectId(McpSessionContext context) {
        if (context.sessionId() == null) return null;
        return sessionRepository.findById(context.sessionId())
                .map(session -> session.getProject().getId())
                .orElse(null);
    }

    /**
     * Builds a document URI from the document type and optional custom name.
     */
    private String buildDocumentUri(DocumentType type, String customName) {
        if (type == DocumentType.CUSTOM && customName != null) {
            return DOCUMENT_URI_PREFIX + "CUSTOM/" + customName;
        }
        return DOCUMENT_URI_PREFIX + type.name();
    }

    /**
     * Returns a human-readable description for a document type.
     */
    private String describeDocumentType(DocumentType type, String customName) {
        return switch (type) {
            case CLAUDE_MD -> "Project-specific AI instructions";
            case CONVENTIONS_MD -> "Coding conventions and standards";
            case ARCHITECTURE_MD -> "System architecture specification";
            case AUDIT_MD -> "Codebase audit results";
            case OPENAPI_YAML -> "OpenAPI specification";
            case CUSTOM -> "Custom document: " + (customName != null ? customName : "unnamed");
        };
    }

    /**
     * Creates a tool definition.
     */
    private McpToolDefinition tool(String name, String description, JsonNode inputSchema) {
        return new McpToolDefinition(name, description, inputSchema);
    }

    /**
     * Creates an empty object schema (no parameters).
     */
    private JsonNode emptySchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return schema;
    }

    /**
     * Creates a new schema builder for defining tool input parameters.
     */
    private SchemaBuilder schema() {
        return new SchemaBuilder(objectMapper);
    }

    /**
     * Fluent builder for JSON Schema objects used in tool definitions.
     */
    static class SchemaBuilder {
        private final ObjectMapper mapper;
        private final ObjectNode properties;
        private final List<String> required = new ArrayList<>();

        SchemaBuilder(ObjectMapper mapper) {
            this.mapper = mapper;
            this.properties = mapper.createObjectNode();
        }

        SchemaBuilder reqStr(String name, String description) {
            properties.putObject(name).put("type", "string").put("description", description);
            required.add(name);
            return this;
        }

        SchemaBuilder optStr(String name, String description) {
            properties.putObject(name).put("type", "string").put("description", description);
            return this;
        }

        SchemaBuilder optInt(String name, String description) {
            properties.putObject(name).put("type", "integer").put("description", description);
            return this;
        }

        SchemaBuilder optBool(String name, String description) {
            properties.putObject(name).put("type", "boolean").put("description", description);
            return this;
        }

        JsonNode build() {
            ObjectNode schema = mapper.createObjectNode();
            schema.put("type", "object");
            schema.set("properties", properties);
            if (!required.isEmpty()) {
                ArrayNode reqArray = schema.putArray("required");
                required.forEach(reqArray::add);
            }
            return schema;
        }
    }
}
