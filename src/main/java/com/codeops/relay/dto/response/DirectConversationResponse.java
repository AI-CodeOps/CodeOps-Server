package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full direct conversation detail. */
public record DirectConversationResponse(
        UUID id,
        UUID teamId,
        ConversationType conversationType,
        String name,
        List<UUID> participantIds,
        Instant lastMessageAt,
        String lastMessagePreview,
        Instant createdAt,
        Instant updatedAt
) {}
