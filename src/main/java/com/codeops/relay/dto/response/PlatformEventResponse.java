package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.PlatformEventType;

import java.time.Instant;
import java.util.UUID;

/** Platform event detail with delivery status. */
public record PlatformEventResponse(
        UUID id,
        PlatformEventType eventType,
        UUID teamId,
        String sourceModule,
        UUID sourceEntityId,
        String title,
        String detail,
        UUID targetChannelId,
        String targetChannelSlug,
        boolean isDelivered,
        Instant deliveredAt,
        Instant createdAt
) {}
