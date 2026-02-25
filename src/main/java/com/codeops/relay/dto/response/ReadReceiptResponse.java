package com.codeops.relay.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Read receipt indicating last read position in a channel. */
public record ReadReceiptResponse(
        UUID channelId,
        UUID userId,
        UUID lastReadMessageId,
        Instant lastReadAt
) {}
