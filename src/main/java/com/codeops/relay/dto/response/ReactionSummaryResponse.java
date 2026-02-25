package com.codeops.relay.dto.response;

import java.util.List;
import java.util.UUID;

/** Aggregated reaction summary for a specific emoji on a message. */
public record ReactionSummaryResponse(
        String emoji,
        long count,
        boolean currentUserReacted,
        List<UUID> userIds
) {}
