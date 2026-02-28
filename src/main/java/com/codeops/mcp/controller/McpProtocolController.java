package com.codeops.mcp.controller;

import com.codeops.config.AppConstants;
import com.codeops.mcp.dto.McpSessionContext;
import com.codeops.mcp.security.McpSecurityService;
import com.codeops.mcp.service.McpProtocolService;
import com.codeops.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for the MCP protocol layer.
 *
 * <p>Exposes the Server-Sent Events (SSE) transport for AI agent connections
 * and a synchronous JSON-RPC endpoint for HTTP transport. The SSE endpoint
 * creates a persistent connection that receives JSON-RPC responses, while
 * the synchronous endpoint returns JSON-RPC responses directly.</p>
 *
 * <p>All endpoints require ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.MCP_API_PREFIX + "/protocol")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class McpProtocolController {

    private final McpProtocolService mcpProtocolService;
    private final McpSecurityService mcpSecurityService;

    /** Active SSE connections keyed by connection ID. */
    private final ConcurrentHashMap<String, SseEmitter> connections = new ConcurrentHashMap<>();

    /**
     * Opens an SSE connection for an AI agent.
     *
     * <p>Creates a new {@link SseEmitter} with a session-based timeout, stores it
     * in the connection map, and sends an initial {@code endpoint} event containing
     * the connection ID. The client uses this connection ID for subsequent message calls.</p>
     *
     * @return the SSE emitter for the connection
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        String connectionId = UUID.randomUUID().toString();
        long timeoutMs = AppConstants.MCP_DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000L;
        SseEmitter emitter = new SseEmitter(timeoutMs);

        connections.put(connectionId, emitter);

        emitter.onCompletion(() -> {
            connections.remove(connectionId);
            log.debug("SSE connection {} completed", connectionId);
        });
        emitter.onError(ex -> {
            connections.remove(connectionId);
            log.warn("SSE connection {} error: {}", connectionId, ex.getMessage());
        });
        emitter.onTimeout(() -> {
            connections.remove(connectionId);
            log.debug("SSE connection {} timed out", connectionId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data(connectionId));
        } catch (Exception e) {
            log.error("Failed to send initial endpoint event for connection {}: {}",
                    connectionId, e.getMessage());
            connections.remove(connectionId);
        }

        log.info("Opened SSE connection {}", connectionId);
        return emitter;
    }

    /**
     * Handles an MCP JSON-RPC message over SSE transport.
     *
     * <p>Extracts the session context from the request, processes the JSON-RPC
     * request through the protocol service, and sends the response via the
     * associated SSE emitter.</p>
     *
     * @param connectionId the SSE connection ID
     * @param jsonRpcBody  the raw JSON-RPC request body
     * @param request      the HTTP servlet request
     */
    @PostMapping("/sse/message")
    public void handleSseMessage(@RequestParam String connectionId,
                                  @RequestBody String jsonRpcBody,
                                  HttpServletRequest request) {
        SseEmitter emitter = connections.get(connectionId);
        if (emitter == null) {
            throw new com.codeops.exception.NotFoundException("SSE connection", "connectionId", connectionId);
        }

        McpSessionContext context = resolveContext(request);
        String response = mcpProtocolService.handleRequest(jsonRpcBody, context);

        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(response));
        } catch (Exception e) {
            log.error("Failed to send SSE message on connection {}: {}", connectionId, e.getMessage());
            connections.remove(connectionId);
        }
    }

    /**
     * Handles an MCP JSON-RPC message synchronously over HTTP transport.
     *
     * <p>Processes the JSON-RPC request and returns the response directly
     * in the HTTP response body.</p>
     *
     * @param jsonRpcBody the raw JSON-RPC request body
     * @param request     the HTTP servlet request
     * @return the JSON-RPC response string
     */
    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public String handleMessage(@RequestBody String jsonRpcBody,
                                 HttpServletRequest request) {
        McpSessionContext context = resolveContext(request);
        return mcpProtocolService.handleRequest(jsonRpcBody, context);
    }

    /**
     * Resolves the MCP session context from the request.
     *
     * <p>First checks for a context set by McpTokenAuthFilter (API token auth).
     * Falls back to building a context from the JWT-authenticated user.</p>
     *
     * @param request the HTTP servlet request
     * @return the resolved session context
     */
    private McpSessionContext resolveContext(HttpServletRequest request) {
        McpSessionContext context = mcpSecurityService.getCurrentContext(request);
        if (context != null) {
            return context;
        }
        UUID userId = SecurityUtils.getCurrentUserId();
        return mcpSecurityService.buildContextFromJwt(userId, null);
    }
}
