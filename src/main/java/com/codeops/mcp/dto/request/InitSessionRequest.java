package com.codeops.mcp.dto.request;

import com.codeops.mcp.entity.enums.McpTransport;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to initialize a new MCP AI development session.
 *
 * @param projectId      the project to work on
 * @param environment    target deployment environment name
 * @param transport      MCP protocol transport type
 * @param timeoutMinutes optional session timeout; defaults to 120
 */
public record InitSessionRequest(
        @NotNull UUID projectId,
        @NotNull String environment,
        @NotNull McpTransport transport,
        Integer timeoutMinutes
) {}
