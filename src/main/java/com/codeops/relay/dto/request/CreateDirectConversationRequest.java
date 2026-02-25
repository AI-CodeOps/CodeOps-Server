package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Request to create a new direct conversation. Current user is auto-added. */
public record CreateDirectConversationRequest(
        @NotEmpty List<UUID> participantIds,
        String name
) {}
