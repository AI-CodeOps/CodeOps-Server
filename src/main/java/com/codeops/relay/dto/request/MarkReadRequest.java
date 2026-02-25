package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request to mark messages as read up to a given message ID. */
public record MarkReadRequest(
        @NotNull UUID lastReadMessageId
) {}
