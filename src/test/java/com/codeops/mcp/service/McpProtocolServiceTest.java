package com.codeops.mcp.service;

import com.codeops.entity.Project;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.dto.McpToolDefinition;
import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.entity.enums.ToolCallStatus;
import com.codeops.mcp.repository.McpSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpProtocolService}.
 *
 * <p>Verifies JSON-RPC 2.0 message parsing, method routing, tool dispatch,
 * resource listing/reading, capability negotiation, and error handling
 * across all five protocol methods and error codes.</p>
 */
@ExtendWith(MockitoExtension.class)
class McpProtocolServiceTest {

    private static final UUID DEVELOPER_PROFILE_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock private ToolProxyService toolProxyService;
    @Mock private McpSessionService sessionService;
    @Mock private DocumentManagementService documentManagementService;
    @Mock private ContextAssemblyService contextAssemblyService;
    @Mock private McpSessionRepository sessionRepository;

    @Spy private ObjectMapper objectMapper;

    @InjectMocks
    private McpProtocolService service;

    private McpSessionContext sessionContext;

    @BeforeEach
    void setUp() {
        sessionContext = new McpSessionContext(
                DEVELOPER_PROFILE_ID, TEAM_ID, USER_ID, SESSION_ID, List.of("tools", "resources"));
        service.init();
    }

    // ── Test Helpers ──

    /**
     * Builds a JSON-RPC 2.0 request string.
     */
    private String jsonRpcRequest(String id, String method, String params) {
        StringBuilder sb = new StringBuilder("{\"jsonrpc\":\"2.0\"");
        if (id != null) {
            sb.append(",\"id\":\"").append(id).append("\"");
        }
        if (method != null) {
            sb.append(",\"method\":\"").append(method).append("\"");
        }
        if (params != null) {
            sb.append(",\"params\":").append(params);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates a mock McpSession with a project for resource resolution.
     */
    private McpSession createSessionWithProject() {
        Project project = new Project();
        project.setId(PROJECT_ID);

        McpSession session = new McpSession();
        session.setId(SESSION_ID);
        session.setProject(project);
        return session;
    }

    // ══════════════════════════════════════════════════════════════
    //                    INITIALIZE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Initialize")
    class InitializeTests {

        @Test
        @DisplayName("returns server info and capabilities")
        void handleRequest_initialize_returnsCapabilities() throws Exception {
            String request = jsonRpcRequest("1", "initialize", "{}");

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(root.path("id").asText()).isEqualTo("1");

            JsonNode result = root.path("result");
            assertThat(result.path("protocolVersion").asText()).isEqualTo("2024-11-05");
            assertThat(result.path("serverInfo").path("name").asText()).isEqualTo("CodeOps MCP Gateway");
            assertThat(result.path("serverInfo").path("version").asText()).isEqualTo("1.0.0");
            assertThat(result.path("capabilities").has("tools")).isTrue();
            assertThat(result.path("capabilities").has("resources")).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    TOOLS/LIST
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tools List")
    class ToolsListTests {

        @Test
        @DisplayName("returns all 34 tool definitions")
        void handleRequest_toolsList_returnsAllTools() throws Exception {
            String request = jsonRpcRequest("2", "tools/list", "{}");

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            JsonNode tools = root.path("result").path("tools");
            assertThat(tools.isArray()).isTrue();
            assertThat(tools.size()).isEqualTo(34);

            // Verify first tool has expected structure
            JsonNode firstTool = tools.get(0);
            assertThat(firstTool.has("name")).isTrue();
            assertThat(firstTool.has("description")).isTrue();
            assertThat(firstTool.has("inputSchema")).isTrue();
        }

        @Test
        @DisplayName("all tool definitions have schemas")
        void buildToolDefinitions_allToolsHaveSchemas() {
            List<McpToolDefinition> definitions = service.buildToolDefinitions();

            assertThat(definitions).hasSize(34);
            for (McpToolDefinition def : definitions) {
                assertThat(def.name()).isNotBlank();
                assertThat(def.description()).isNotBlank();
                assertThat(def.inputSchema()).isNotNull();
                assertThat(def.inputSchema().path("type").asText()).isEqualTo("object");
                assertThat(def.inputSchema().has("properties")).isTrue();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    TOOLS/CALL
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tools Call")
    class ToolsCallTests {

        @Test
        @DisplayName("delegates tool call to ToolProxyService")
        void handleRequest_toolsCall_delegatesToProxy() throws Exception {
            String params = "{\"name\":\"registry_listServices\",\"arguments\":{\"search\":\"myapp\"}}";
            String request = jsonRpcRequest("3", "tools/call", params);

            SessionToolCallResponse toolResponse = new SessionToolCallResponse(
                    UUID.randomUUID(), "registry.listServices", "registry",
                    null, "{\"services\":[]}", ToolCallStatus.SUCCESS, 50L, null, NOW, NOW);

            when(toolProxyService.executeTool(eq(SESSION_ID), eq(TEAM_ID), eq(USER_ID), any()))
                    .thenReturn(toolResponse);

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            verify(toolProxyService).executeTool(eq(SESSION_ID), eq(TEAM_ID), eq(USER_ID), any());
            assertThat(root.path("id").asText()).isEqualTo("3");
            assertThat(root.has("error")).isFalse();
        }

        @Test
        @DisplayName("returns MCP content array format for success")
        void handleRequest_toolsCall_returnsContentFormat() throws Exception {
            String params = "{\"name\":\"fleet_listContainers\",\"arguments\":{}}";
            String request = jsonRpcRequest("4", "tools/call", params);

            SessionToolCallResponse toolResponse = new SessionToolCallResponse(
                    UUID.randomUUID(), "fleet.listContainers", "fleet",
                    null, "[{\"id\":\"abc\"}]", ToolCallStatus.SUCCESS, 10L, null, NOW, NOW);

            when(toolProxyService.executeTool(eq(SESSION_ID), eq(TEAM_ID), eq(USER_ID), any()))
                    .thenReturn(toolResponse);

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            JsonNode result = root.path("result");
            JsonNode content = result.path("content");
            assertThat(content.isArray()).isTrue();
            assertThat(content.size()).isEqualTo(1);
            assertThat(content.get(0).path("type").asText()).isEqualTo("text");
            assertThat(content.get(0).path("text").asText()).isEqualTo("[{\"id\":\"abc\"}]");
            assertThat(result.has("isError")).isFalse();
        }

        @Test
        @DisplayName("returns error content when tool call fails")
        void handleRequest_toolsCallError_returnsErrorContent() throws Exception {
            String params = "{\"name\":\"registry_getService\",\"arguments\":{\"serviceId\":\"bad-id\"}}";
            String request = jsonRpcRequest("5", "tools/call", params);

            SessionToolCallResponse toolResponse = new SessionToolCallResponse(
                    UUID.randomUUID(), "registry.getService", "registry",
                    null, null, ToolCallStatus.FAILURE, 5L, "Service not found", NOW, NOW);

            when(toolProxyService.executeTool(eq(SESSION_ID), eq(TEAM_ID), eq(USER_ID), any()))
                    .thenReturn(toolResponse);

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            JsonNode result = root.path("result");
            assertThat(result.path("isError").asBoolean()).isTrue();
            JsonNode content = result.path("content");
            assertThat(content.get(0).path("text").asText()).isEqualTo("Service not found");
        }

        @Test
        @DisplayName("returns invalid params when tool name missing")
        void handleRequest_invalidParams_returnsInvalidParams() throws Exception {
            String params = "{\"arguments\":{}}";
            String request = jsonRpcRequest("6", "tools/call", params);

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("error").path("code").asInt()).isEqualTo(-32602);
            assertThat(root.path("error").path("message").asText()).contains("Missing tool name");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    RESOURCES
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Resources")
    class ResourcesTests {

        @Test
        @DisplayName("resources/list returns project documents as resources")
        void handleRequest_resourcesList_returnsDocuments() throws Exception {
            String request = jsonRpcRequest("7", "resources/list", "{}");

            McpSession session = createSessionWithProject();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            List<ProjectDocumentResponse> docs = List.of(
                    new ProjectDocumentResponse(UUID.randomUUID(), DocumentType.CLAUDE_MD, null,
                            AuthorType.HUMAN, null, false, null, PROJECT_ID, "Adam", NOW, NOW),
                    new ProjectDocumentResponse(UUID.randomUUID(), DocumentType.OPENAPI_YAML, null,
                            AuthorType.AI, SESSION_ID, false, null, PROJECT_ID, "Claude", NOW, NOW),
                    new ProjectDocumentResponse(UUID.randomUUID(), DocumentType.CUSTOM, "deployment-guide",
                            AuthorType.HUMAN, null, false, null, PROJECT_ID, "Adam", NOW, NOW));

            when(documentManagementService.getProjectDocuments(PROJECT_ID)).thenReturn(docs);

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            JsonNode resources = root.path("result").path("resources");
            assertThat(resources.isArray()).isTrue();
            assertThat(resources.size()).isEqualTo(3);

            // CLAUDE_MD resource
            assertThat(resources.get(0).path("uri").asText()).isEqualTo("document:///CLAUDE_MD");
            assertThat(resources.get(0).path("mimeType").asText()).isEqualTo("text/markdown");

            // OPENAPI_YAML resource
            assertThat(resources.get(1).path("uri").asText()).isEqualTo("document:///OPENAPI_YAML");
            assertThat(resources.get(1).path("mimeType").asText()).isEqualTo("application/yaml");

            // CUSTOM resource
            assertThat(resources.get(2).path("uri").asText()).isEqualTo("document:///CUSTOM/deployment-guide");
            assertThat(resources.get(2).path("name").asText()).isEqualTo("deployment-guide");
        }

        @Test
        @DisplayName("resources/read returns document content")
        void handleRequest_resourcesRead_returnsContent() throws Exception {
            String params = "{\"uri\":\"document:///CLAUDE_MD\"}";
            String request = jsonRpcRequest("8", "resources/read", params);

            McpSession session = createSessionWithProject();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            ProjectDocumentDetailResponse doc = new ProjectDocumentDetailResponse(
                    UUID.randomUUID(), DocumentType.CLAUDE_MD, null,
                    "# Project Instructions\nUse Spring Boot.",
                    AuthorType.HUMAN, null, false, null, PROJECT_ID, "Adam", List.of(), NOW, NOW);

            when(documentManagementService.getDocument(PROJECT_ID, DocumentType.CLAUDE_MD)).thenReturn(doc);

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            JsonNode contents = root.path("result").path("contents");
            assertThat(contents.isArray()).isTrue();
            assertThat(contents.size()).isEqualTo(1);
            assertThat(contents.get(0).path("uri").asText()).isEqualTo("document:///CLAUDE_MD");
            assertThat(contents.get(0).path("mimeType").asText()).isEqualTo("text/markdown");
            assertThat(contents.get(0).path("text").asText()).isEqualTo("# Project Instructions\nUse Spring Boot.");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    ERROR HANDLING
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("returns method not found for unknown method")
        void handleRequest_unknownMethod_returnsMethodNotFound() throws Exception {
            String request = jsonRpcRequest("10", "unknown/method", "{}");

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(root.path("id").asText()).isEqualTo("10");
            assertThat(root.path("error").path("code").asInt()).isEqualTo(-32601);
            assertThat(root.path("error").path("message").asText()).contains("Method not found");
        }

        @Test
        @DisplayName("returns parse error for invalid JSON")
        void handleRequest_invalidJsonRpc_returnsParseError() throws Exception {
            String response = service.handleRequest("not valid json{{{", sessionContext);
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("error").path("code").asInt()).isEqualTo(-32700);
            assertThat(root.path("error").path("message").asText()).contains("Parse error");
        }

        @Test
        @DisplayName("returns invalid request for missing jsonrpc version")
        void handleRequest_missingVersion_returnsInvalidRequest() throws Exception {
            String request = "{\"id\":\"11\",\"method\":\"initialize\"}";

            String response = service.handleRequest(request, sessionContext);
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("id").asText()).isEqualTo("11");
            assertThat(root.path("error").path("code").asInt()).isEqualTo(-32600);
            assertThat(root.path("error").path("message").asText()).contains("Invalid or missing JSON-RPC version");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                    RESPONSE BUILDERS
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Response Builders")
    class ResponseBuilderTests {

        @Test
        @DisplayName("buildSuccessResponse produces valid JSON-RPC envelope")
        void buildSuccessResponse_validJsonRpc() throws Exception {
            String response = service.buildSuccessResponse("req-1",
                    java.util.Map.of("key", "value"));
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(root.path("id").asText()).isEqualTo("req-1");
            assertThat(root.path("result").path("key").asText()).isEqualTo("value");
            assertThat(root.has("error")).isFalse();
        }

        @Test
        @DisplayName("buildErrorResponse produces valid JSON-RPC error envelope")
        void buildErrorResponse_validJsonRpc() throws Exception {
            String response = service.buildErrorResponse("req-2", -32603, "Something broke");
            JsonNode root = objectMapper.readTree(response);

            assertThat(root.path("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(root.path("id").asText()).isEqualTo("req-2");
            assertThat(root.path("error").path("code").asInt()).isEqualTo(-32603);
            assertThat(root.path("error").path("message").asText()).isEqualTo("Something broke");
            assertThat(root.has("result")).isFalse();
        }

        @Test
        @DisplayName("buildServerCapabilities includes tools and resources")
        void buildServerCapabilities_includesToolsAndResources() {
            JsonNode capabilities = service.buildServerCapabilities();

            assertThat(capabilities.has("tools")).isTrue();
            assertThat(capabilities.has("resources")).isTrue();
            assertThat(capabilities.path("tools").isObject()).isTrue();
            assertThat(capabilities.path("resources").isObject()).isTrue();
        }
    }
}
