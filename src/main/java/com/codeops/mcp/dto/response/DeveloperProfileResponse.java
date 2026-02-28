package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.Environment;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an MCP developer profile.
 *
 * @param id                 profile ID
 * @param displayName        override display name for MCP context
 * @param bio                developer bio
 * @param defaultEnvironment default deployment environment
 * @param preferencesJson    JSON string of AI preferences
 * @param timezone           IANA timezone identifier
 * @param isActive           whether the profile is active
 * @param teamId             owning team ID
 * @param userId             associated user ID
 * @param userDisplayName    user's display name
 * @param createdAt          creation timestamp
 * @param updatedAt          last update timestamp
 */
public record DeveloperProfileResponse(
        UUID id,
        String displayName,
        String bio,
        Environment defaultEnvironment,
        String preferencesJson,
        String timezone,
        boolean isActive,
        UUID teamId,
        UUID userId,
        String userDisplayName,
        Instant createdAt,
        Instant updatedAt
) {}
