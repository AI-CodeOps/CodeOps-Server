package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request to pin a message in a channel. */
public record PinMessageRequest(
        @NotNull UUID messageId
) {}
