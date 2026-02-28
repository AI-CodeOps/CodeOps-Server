package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.ToolCallStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single tool call within an MCP session.
 *
 * @param id           tool call ID
 * @param toolName     fully qualified tool name
 * @param toolCategory tool category
 * @param requestJson  tool call arguments
 * @param responseJson tool call result
 * @param status       result status
 * @param durationMs   execution time in milliseconds
 * @param errorMessage error message on failure
 * @param calledAt     when the call was made
 * @param createdAt    creation timestamp
 */
public record SessionToolCallResponse(
        UUID id,
        String toolName,
        String toolCategory,
        String requestJson,
        String responseJson,
        ToolCallStatus status,
        long durationMs,
        String errorMessage,
        Instant calledAt,
        Instant createdAt
) {}
