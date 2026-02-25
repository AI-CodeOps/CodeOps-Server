package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to add an emoji reaction to a message. */
public record AddReactionRequest(
        @NotBlank @Size(max = 50) String emoji
) {}
