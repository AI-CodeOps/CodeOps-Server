package com.codeops.mcp.entity.enums;

/**
 * Result status of an MCP tool call.
 *
 * <p>Indicates whether a tool invocation by an AI agent
 * succeeded, failed, timed out, or was unauthorized.</p>
 */
public enum ToolCallStatus {
    /** Tool call completed successfully. */
    SUCCESS,
    /** Tool call failed with an error. */
    FAILURE,
    /** Tool call exceeded the time limit. */
    TIMEOUT,
    /** Tool call was rejected due to insufficient permissions. */
    UNAUTHORIZED
}
