package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.ReactionType;

import java.time.Instant;
import java.util.UUID;

/** Individual reaction detail with user info. */
public record ReactionResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        String emoji,
        ReactionType reactionType,
        Instant createdAt
) {}
