package com.codeops.relay.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Thread metadata with recent replies. */
public record MessageThreadResponse(
        UUID rootMessageId,
        UUID channelId,
        int replyCount,
        Instant lastReplyAt,
        UUID lastReplyBy,
        List<UUID> participantIds,
        List<MessageResponse> replies
) {}
