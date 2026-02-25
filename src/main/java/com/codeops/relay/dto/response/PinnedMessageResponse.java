package com.codeops.relay.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Pinned message detail with the full message content. */
public record PinnedMessageResponse(
        UUID id,
        UUID messageId,
        UUID channelId,
        MessageResponse message,
        UUID pinnedBy,
        Instant createdAt
) {}
