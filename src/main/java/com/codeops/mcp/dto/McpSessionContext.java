package com.codeops.mcp.dto;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated context for an MCP connection.
 *
 * <p>Created by the auth filter when an MCP API token or JWT is validated.
 * Carries the identity and permission scope for all subsequent protocol
 * operations within the connection.</p>
 *
 * @param developerProfileId the authenticated developer profile ID
 * @param teamId             the team the profile belongs to
 * @param userId             the underlying user ID
 * @param sessionId          the active MCP session ID (null until initSession called)
 * @param allowedScopes      permission scopes from the API token
 */
public record McpSessionContext(
        UUID developerProfileId,
        UUID teamId,
        UUID userId,
        UUID sessionId,
        List<String> allowedScopes
) {}
