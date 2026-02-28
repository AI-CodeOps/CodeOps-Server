package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response DTO for an MCP session including tool calls and result.
 *
 * @param id              session ID
 * @param status          current session status
 * @param projectName     denormalized project name
 * @param developerName   denormalized developer display name
 * @param environment     target deployment environment
 * @param transport       MCP transport type
 * @param startedAt       when the session became active
 * @param completedAt     when the session completed or failed
 * @param lastActivityAt  last tool call timestamp
 * @param timeoutMinutes  max session duration
 * @param totalToolCalls  running count of tool calls
 * @param errorMessage    error message on failure
 * @param toolCalls       list of tool calls made during the session
 * @param result          session result summary
 * @param createdAt       creation timestamp
 * @param updatedAt       last update timestamp
 */
public record McpSessionDetailResponse(
        UUID id,
        SessionStatus status,
        String projectName,
        String developerName,
        Environment environment,
        McpTransport transport,
        Instant startedAt,
        Instant completedAt,
        Instant lastActivityAt,
        int timeoutMinutes,
        int totalToolCalls,
        String errorMessage,
        List<SessionToolCallResponse> toolCalls,
        SessionResultResponse result,
        Instant createdAt,
        Instant updatedAt
) {}
