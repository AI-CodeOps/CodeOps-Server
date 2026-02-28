package com.codeops.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP tool definition with JSON Schema for tool discovery.
 *
 * <p>Used by the protocol layer to advertise available tools to AI agents
 * via the {@code tools/list} JSON-RPC method. Each definition includes
 * a JSON Schema describing the tool's input parameters.</p>
 *
 * @param name        tool name in underscore format (e.g., "registry_listServices")
 * @param description human-readable description of the tool
 * @param inputSchema JSON Schema describing the tool's input parameters
 */
public record McpToolDefinition(
        String name,
        String description,
        JsonNode inputSchema
) {}
