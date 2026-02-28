package com.codeops.mcp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Request to create a new MCP API token for authenticating AI agent connections.
 *
 * @param name      human-readable token name (e.g., "Claude Code Laptop")
 * @param expiresAt optional expiration timestamp; null means never expires
 * @param scopes    optional list of allowed tool categories; null means all
 */
public record CreateApiTokenRequest(
        @NotBlank @Size(max = 200) String name,
        Instant expiresAt,
        List<String> scopes
) {}
