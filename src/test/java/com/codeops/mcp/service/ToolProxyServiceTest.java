package com.codeops.mcp.service;

import com.codeops.courier.service.CollectionRunnerService;
import com.codeops.courier.service.HistoryService;
import com.codeops.courier.service.ImportService;
import com.codeops.courier.service.RequestProxyService;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.fleet.service.ContainerManagementService;
import com.codeops.fleet.service.SolutionProfileService;
import com.codeops.logger.service.AlertService;
import com.codeops.logger.service.AnomalyDetectionService;
import com.codeops.logger.service.LogQueryService;
import com.codeops.logger.service.MetricsService;
import com.codeops.logger.service.TraceService;
import com.codeops.mcp.dto.request.ToolCallRequest;
import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.enums.ToolCallStatus;
import com.codeops.registry.service.ApiRouteService;
import com.codeops.registry.service.ConfigEngineService;
import com.codeops.registry.service.DependencyGraphService;
import com.codeops.registry.service.PortAllocationService;
import com.codeops.registry.service.ServiceRegistryService;
import com.codeops.registry.service.SolutionService;
import com.codeops.registry.service.TopologyService;
import com.codeops.relay.service.ChannelService;
import com.codeops.relay.service.MessageService;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ToolProxyService}.
 *
 * <p>Verifies tool dispatch across all 5 modules (Registry, Logger, Courier,
 * Relay, Fleet), error handling, authorization failures, unknown tool rejection,
 * and response truncation.</p>
 */
@ExtendWith(MockitoExtension.class)
class ToolProxyServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock private McpSessionService sessionService;

    // Registry
    @Mock private ServiceRegistryService serviceRegistryService;
    @Mock private DependencyGraphService dependencyGraphService;
    @Mock private PortAllocationService portAllocationService;
    @Mock private ApiRouteService apiRouteService;
    @Mock private ConfigEngineService configEngineService;
    @Mock private SolutionService solutionService;
    @Mock private TopologyService topologyService;

    // Logger
    @Mock private LogQueryService logQueryService;
    @Mock private MetricsService metricsService;
    @Mock private TraceService traceService;
    @Mock private AlertService alertService;
    @Mock private AnomalyDetectionService anomalyDetectionService;

    // Courier
    @Mock private RequestProxyService requestProxyService;
    @Mock private CollectionRunnerService collectionRunnerService;
    @Mock private HistoryService historyService;
    @Mock private ImportService importService;

    // Relay
    @Mock private MessageService messageService;
    @Mock private ChannelService channelService;

    // Fleet
    @Mock private ContainerManagementService containerManagementService;
    @Mock private SolutionProfileService solutionProfileService;

    @Spy private ObjectMapper objectMapper;

    @InjectMocks
    private ToolProxyService service;

    @BeforeEach
    void setUp() {
        service.registerTools();
    }

    // ── Test Helpers ──

    private SessionToolCallResponse createToolCallResponse(String toolName, ToolCallStatus status) {
        return new SessionToolCallResponse(
                UUID.randomUUID(), toolName, toolName.split("\\.")[0],
                null, null, status, 10L, null, NOW, NOW);
    }

    private void mockRecordSuccess(ToolCallRequest request) {
        when(sessionService.recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.SUCCESS),
                anyLong(), any(), isNull()))
                .thenReturn(createToolCallResponse(request.toolName(), ToolCallStatus.SUCCESS));
    }

    private void mockRecordFailure(ToolCallRequest request) {
        when(sessionService.recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.FAILURE),
                anyLong(), isNull(), anyString()))
                .thenReturn(createToolCallResponse(request.toolName(), ToolCallStatus.FAILURE));
    }

    private void mockRecordUnauthorized(ToolCallRequest request) {
        when(sessionService.recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.UNAUTHORIZED),
                anyLong(), isNull(), anyString()))
                .thenReturn(createToolCallResponse(request.toolName(), ToolCallStatus.UNAUTHORIZED));
    }

    // ══════════════════════════════════════════════════════════════
    //                      CORE DISPATCH
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Core Dispatch")
    class CoreDispatchTests {

        @Test
        @DisplayName("returns FAILURE for unknown tool name")
        void executeTool_unknownTool_returnsFailure() {
            ToolCallRequest request = new ToolCallRequest("unknown.tool", "unknown", "{}");

            when(sessionService.recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.FAILURE),
                    eq(0L), isNull(), eq("Unknown tool: unknown.tool")))
                    .thenReturn(createToolCallResponse("unknown.tool", ToolCallStatus.FAILURE));

            SessionToolCallResponse response = service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(ToolCallStatus.FAILURE);
            verify(sessionService).recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.FAILURE),
                    eq(0L), isNull(), eq("Unknown tool: unknown.tool"));
        }

        @Test
        @DisplayName("returns FAILURE when service throws exception")
        void executeTool_serviceException_returnsFailure() {
            ToolCallRequest request = new ToolCallRequest("registry.getService", "registry",
                    "{\"serviceId\":\"" + UUID.randomUUID() + "\"}");

            when(serviceRegistryService.getService(any(UUID.class)))
                    .thenThrow(new NotFoundException("ServiceRegistration", UUID.randomUUID()));
            mockRecordFailure(request);

            SessionToolCallResponse response = service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(ToolCallStatus.FAILURE);
        }

        @Test
        @DisplayName("returns UNAUTHORIZED when service throws AuthorizationException")
        void executeTool_authorizationException_returnsUnauthorized() {
            ToolCallRequest request = new ToolCallRequest("registry.getTopology", "registry", "{}");

            when(topologyService.getTopology(TEAM_ID))
                    .thenThrow(new AuthorizationException("Not authorized"));
            mockRecordUnauthorized(request);

            SessionToolCallResponse response = service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(ToolCallStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("truncates response exceeding max length")
        void executeTool_longResponse_truncatesResponse() {
            UUID containerId = UUID.randomUUID();
            ToolCallRequest request = new ToolCallRequest("fleet.getContainerLogs", "fleet",
                    "{\"containerId\":\"" + containerId + "\"}");

            // Build a log string exceeding MCP_MAX_TOOL_CALL_RESPONSE_LENGTH (50000)
            String longLogs = "x".repeat(60000);
            when(containerManagementService.getContainerLogs(TEAM_ID, containerId, 100, true))
                    .thenReturn(longLogs);

            when(sessionService.recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.SUCCESS),
                    anyLong(), argThat(json -> json != null && json.endsWith("...[TRUNCATED]")), isNull()))
                    .thenReturn(createToolCallResponse(request.toolName(), ToolCallStatus.SUCCESS));

            SessionToolCallResponse response = service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            assertThat(response).isNotNull();
            verify(sessionService).recordToolCall(eq(SESSION_ID), eq(request), eq(ToolCallStatus.SUCCESS),
                    anyLong(), argThat(json -> json != null && json.contains("...[TRUNCATED]")), isNull());
        }

        @Test
        @DisplayName("getRegisteredToolNames returns all 34 tools")
        void getRegisteredToolNames_returnsAll34() {
            assertThat(service.getRegisteredToolNames()).hasSize(34);
            assertThat(service.getRegisteredToolNames()).contains(
                    "registry.listServices", "logger.queryLogs", "courier.sendRequest",
                    "relay.sendMessage", "fleet.listContainers");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                      REGISTRY MODULE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Registry Module")
    class RegistryModuleTests {

        @Test
        @DisplayName("registry.listServices delegates to ServiceRegistryService")
        void listServices_delegates() {
            ToolCallRequest request = new ToolCallRequest("registry.listServices", "registry",
                    "{\"search\":\"myapp\"}");

            doReturn(null).when(serviceRegistryService).getServicesForTeam(eq(TEAM_ID), isNull(),
                    isNull(), eq("myapp"), any(Pageable.class));
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(serviceRegistryService).getServicesForTeam(eq(TEAM_ID), isNull(), isNull(),
                    eq("myapp"), any(Pageable.class));
        }

        @Test
        @DisplayName("registry.getService delegates with parsed serviceId")
        void getService_delegates() {
            UUID serviceId = UUID.randomUUID();
            ToolCallRequest request = new ToolCallRequest("registry.getService", "registry",
                    "{\"serviceId\":\"" + serviceId + "\"}");

            when(serviceRegistryService.getService(serviceId)).thenReturn(null);
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(serviceRegistryService).getService(serviceId);
        }

        @Test
        @DisplayName("registry.getDependencyGraph uses team context")
        void getDependencyGraph_usesTeamContext() {
            ToolCallRequest request = new ToolCallRequest("registry.getDependencyGraph", "registry", "{}");

            when(dependencyGraphService.getDependencyGraph(TEAM_ID)).thenReturn(null);
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(dependencyGraphService).getDependencyGraph(TEAM_ID);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                      LOGGER MODULE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Logger Module")
    class LoggerModuleTests {

        @Test
        @DisplayName("logger.searchLogs extracts search parameters")
        void searchLogs_extractsParams() {
            ToolCallRequest request = new ToolCallRequest("logger.searchLogs", "logger",
                    "{\"searchTerm\":\"error\",\"page\":0,\"size\":25}");

            doReturn(null).when(logQueryService).search(eq("error"), eq(TEAM_ID), isNull(),
                    isNull(), eq(0), eq(25));
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(logQueryService).search(eq("error"), eq(TEAM_ID), isNull(), isNull(), eq(0), eq(25));
        }

        @Test
        @DisplayName("logger.getTraceFlow delegates with correlationId")
        void getTraceFlow_delegates() {
            ToolCallRequest request = new ToolCallRequest("logger.getTraceFlow", "logger",
                    "{\"correlationId\":\"abc-123\"}");

            when(traceService.getTraceFlow("abc-123")).thenReturn(null);
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(traceService).getTraceFlow("abc-123");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                      COURIER MODULE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Courier Module")
    class CourierModuleTests {

        @Test
        @DisplayName("courier.getHistory delegates with pagination")
        void getHistory_delegates() {
            ToolCallRequest request = new ToolCallRequest("courier.getHistory", "courier",
                    "{\"page\":1,\"size\":20}");

            doReturn(null).when(historyService).getHistory(TEAM_ID, 1, 20);
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(historyService).getHistory(TEAM_ID, 1, 20);
        }

        @Test
        @DisplayName("courier.getRunResult delegates with runResultId")
        void getRunResult_delegates() {
            UUID runResultId = UUID.randomUUID();
            ToolCallRequest request = new ToolCallRequest("courier.getRunResult", "courier",
                    "{\"runResultId\":\"" + runResultId + "\"}");

            when(collectionRunnerService.getRunResult(runResultId, TEAM_ID)).thenReturn(null);
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(collectionRunnerService).getRunResult(runResultId, TEAM_ID);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                      RELAY MODULE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Relay Module")
    class RelayModuleTests {

        @Test
        @DisplayName("relay.getChannels delegates with team and user context")
        void getChannels_delegates() {
            ToolCallRequest request = new ToolCallRequest("relay.getChannels", "relay", "{}");

            when(channelService.getChannelsByTeam(TEAM_ID, USER_ID)).thenReturn(List.of());
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(channelService).getChannelsByTeam(TEAM_ID, USER_ID);
        }

        @Test
        @DisplayName("relay.getUnreadCounts delegates with team and user context")
        void getUnreadCounts_delegates() {
            ToolCallRequest request = new ToolCallRequest("relay.getUnreadCounts", "relay", "{}");

            when(messageService.getUnreadCounts(TEAM_ID, USER_ID)).thenReturn(List.of());
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(messageService).getUnreadCounts(TEAM_ID, USER_ID);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                      FLEET MODULE
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Fleet Module")
    class FleetModuleTests {

        @Test
        @DisplayName("fleet.listContainers delegates with team context")
        void listContainers_delegates() {
            ToolCallRequest request = new ToolCallRequest("fleet.listContainers", "fleet", "{}");

            when(containerManagementService.listContainers(TEAM_ID)).thenReturn(List.of());
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(containerManagementService).listContainers(TEAM_ID);
        }

        @Test
        @DisplayName("fleet.getContainerLogs extracts containerId and defaults")
        void getContainerLogs_extractsParams() {
            UUID containerId = UUID.randomUUID();
            ToolCallRequest request = new ToolCallRequest("fleet.getContainerLogs", "fleet",
                    "{\"containerId\":\"" + containerId + "\",\"tailLines\":200}");

            when(containerManagementService.getContainerLogs(TEAM_ID, containerId, 200, true))
                    .thenReturn("log output here");
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(containerManagementService).getContainerLogs(TEAM_ID, containerId, 200, true);
        }

        @Test
        @DisplayName("fleet.startSolution delegates and returns status map")
        void startSolution_delegates() {
            UUID solutionProfileId = UUID.randomUUID();
            ToolCallRequest request = new ToolCallRequest("fleet.startSolution", "fleet",
                    "{\"solutionProfileId\":\"" + solutionProfileId + "\"}");

            doNothing().when(solutionProfileService).startSolution(TEAM_ID, solutionProfileId);
            mockRecordSuccess(request);

            service.executeTool(SESSION_ID, TEAM_ID, USER_ID, request);

            verify(solutionProfileService).startSolution(TEAM_ID, solutionProfileId);
        }
    }
}
