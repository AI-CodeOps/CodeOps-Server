package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/** Channel member detail including display name and role. */
public record ChannelMemberResponse(
        UUID id,
        UUID channelId,
        UUID userId,
        String userDisplayName,
        MemberRole role,
        boolean isMuted,
        Instant lastReadAt,
        Instant joinedAt
) {}
