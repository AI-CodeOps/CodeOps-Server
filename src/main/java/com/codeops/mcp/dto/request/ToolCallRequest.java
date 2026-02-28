package com.codeops.mcp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request representing an MCP tool invocation by an AI agent.
 *
 * @param toolName      fully qualified tool name (e.g., "registry.listServices")
 * @param toolCategory  tool category (e.g., "registry", "fleet", "logger")
 * @param argumentsJson tool call arguments as JSON string
 */
public record ToolCallRequest(
        @NotBlank @Size(max = 200) String toolName,
        @NotBlank @Size(max = 100) String toolCategory,
        String argumentsJson
) {}
