package com.codeops.mcp.entity.enums;

/**
 * MCP protocol transport type.
 *
 * <p>Defines how the AI agent communicates with the MCP Gateway
 * during a development session.</p>
 */
public enum McpTransport {
    /** Server-Sent Events transport. */
    SSE,
    /** HTTP request/response transport. */
    HTTP,
    /** Standard I/O transport (local subprocess). */
    STDIO
}
