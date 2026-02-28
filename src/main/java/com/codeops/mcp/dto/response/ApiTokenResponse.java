package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.TokenStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an MCP API token (excludes the token hash for security).
 *
 * @param id          token ID
 * @param name        human-readable token name
 * @param tokenPrefix display prefix (e.g., "mcp_a1b2...")
 * @param status      current token status
 * @param lastUsedAt  last authentication timestamp
 * @param expiresAt   expiration timestamp
 * @param scopesJson  JSON array of allowed tool categories
 * @param createdAt   creation timestamp
 */
public record ApiTokenResponse(
        UUID id,
        String name,
        String tokenPrefix,
        TokenStatus status,
        Instant lastUsedAt,
        Instant expiresAt,
        String scopesJson,
        Instant createdAt
) {}
