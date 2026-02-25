package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.PresenceStatus;

import java.time.Instant;
import java.util.UUID;

/** User presence status with display name and custom status message. */
public record UserPresenceResponse(
        UUID userId,
        String userDisplayName,
        UUID teamId,
        PresenceStatus status,
        String statusMessage,
        Instant lastSeenAt,
        Instant updatedAt
) {}
