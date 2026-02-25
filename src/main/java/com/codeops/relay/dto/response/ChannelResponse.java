package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.ChannelType;

import java.time.Instant;
import java.util.UUID;

/** Full channel detail response. */
public record ChannelResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String topic,
        ChannelType channelType,
        UUID teamId,
        UUID projectId,
        UUID serviceId,
        boolean isArchived,
        UUID createdBy,
        int memberCount,
        Instant createdAt,
        Instant updatedAt
) {}
