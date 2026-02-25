package com.codeops.relay.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Search result for a message found in a channel. */
public record ChannelSearchResultResponse(
        UUID messageId,
        UUID channelId,
        String channelName,
        UUID senderId,
        String senderDisplayName,
        String contentSnippet,
        Instant createdAt
) {}
