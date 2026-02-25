package com.codeops.relay.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Unread message count for a specific channel. */
public record UnreadCountResponse(
        UUID channelId,
        String channelName,
        String channelSlug,
        long unreadCount,
        Instant lastReadAt
) {}
