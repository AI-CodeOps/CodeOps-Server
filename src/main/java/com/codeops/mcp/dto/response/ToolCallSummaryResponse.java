package com.codeops.mcp.dto.response;

/**
 * Response DTO for a tool call usage summary within a session.
 *
 * @param toolName  fully qualified tool name
 * @param callCount number of times the tool was called
 */
public record ToolCallSummaryResponse(
        String toolName,
        long callCount
) {}
