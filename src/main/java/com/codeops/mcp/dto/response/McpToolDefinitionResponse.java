package com.codeops.mcp.dto.response;

/**
 * Response DTO describing an available MCP tool for AI agents.
 *
 * @param name        tool name (e.g., "registry.listServices")
 * @param description human-readable description of the tool
 * @param category    tool category (e.g., "registry", "fleet")
 * @param inputSchema JSON schema describing the tool's input parameters
 */
public record McpToolDefinitionResponse(
        String name,
        String description,
        String category,
        String inputSchema
) {}
