package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to edit a previously sent message. */
public record UpdateMessageRequest(
        @NotBlank @Size(max = 10000) String content
) {}
