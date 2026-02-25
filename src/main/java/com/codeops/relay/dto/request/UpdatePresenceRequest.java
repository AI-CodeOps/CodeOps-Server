package com.codeops.relay.dto.request;

import com.codeops.relay.entity.enums.PresenceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request to update the current user's presence status. */
public record UpdatePresenceRequest(
        @NotNull PresenceStatus status,
        @Size(max = 200) String statusMessage
) {}
