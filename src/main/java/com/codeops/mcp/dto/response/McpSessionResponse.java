package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight list-view response DTO for an MCP session.
 *
 * @param id             session ID
 * @param status         current session status
 * @param projectName    denormalized project name
 * @param developerName  denormalized developer display name
 * @param environment    target deployment environment
 * @param transport      MCP transport type
 * @param startedAt      when the session became active
 * @param completedAt    when the session completed or failed
 * @param totalToolCalls running count of tool calls
 * @param createdAt      creation timestamp
 */
public record McpSessionResponse(
        UUID id,
        SessionStatus status,
        String projectName,
        String developerName,
        Environment environment,
        McpTransport transport,
        Instant startedAt,
        Instant completedAt,
        int totalToolCalls,
        Instant createdAt
) {}
