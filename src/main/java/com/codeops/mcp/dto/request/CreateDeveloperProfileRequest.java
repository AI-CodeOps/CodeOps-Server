package com.codeops.mcp.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request to create an MCP developer profile for a user within a team.
 *
 * @param displayName        override of user's name for MCP context
 * @param bio                developer bio for AI context
 * @param defaultEnvironment default deployment environment name
 * @param preferencesJson    JSON string of personal AI preferences
 * @param timezone           IANA timezone identifier (e.g., "America/Chicago")
 */
public record CreateDeveloperProfileRequest(
        @Size(max = 200) String displayName,
        @Size(max = 2000) String bio,
        String defaultEnvironment,
        String preferencesJson,
        @Size(max = 50) String timezone
) {}
