package com.codeops.relay.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Real-time typing indicator for a user in a channel. */
public record TypingIndicatorResponse(
        UUID channelId,
        UUID userId,
        String userDisplayName,
        Instant timestamp
) {}
