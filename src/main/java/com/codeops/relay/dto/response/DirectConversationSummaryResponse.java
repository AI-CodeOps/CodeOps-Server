package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lightweight DM conversation summary for the conversation list. */
public record DirectConversationSummaryResponse(
        UUID id,
        ConversationType conversationType,
        String name,
        List<UUID> participantIds,
        List<String> participantDisplayNames,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount
) {}
