package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.ChannelType;

import java.time.Instant;
import java.util.UUID;

/** Lightweight channel summary for sidebar and list views. */
public record ChannelSummaryResponse(
        UUID id,
        String name,
        String slug,
        ChannelType channelType,
        String topic,
        boolean isArchived,
        int memberCount,
        long unreadCount,
        Instant lastMessageAt
) {}
