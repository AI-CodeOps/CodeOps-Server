package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.TokenStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO returned only at token creation time, including the raw token value.
 *
 * <p>The raw token is only available at creation. After this response,
 * the token can never be retrieved again — only the prefix is stored.</p>
 *
 * @param id          token ID
 * @param name        human-readable token name
 * @param tokenPrefix display prefix (e.g., "mcp_a1b2...")
 * @param rawToken    the actual token value (only returned at creation)
 * @param status      current token status
 * @param expiresAt   expiration timestamp
 * @param scopesJson  JSON array of allowed tool categories
 * @param createdAt   creation timestamp
 */
public record ApiTokenCreatedResponse(
        UUID id,
        String name,
        String tokenPrefix,
        String rawToken,
        TokenStatus status,
        Instant expiresAt,
        String scopesJson,
        Instant createdAt
) {}
