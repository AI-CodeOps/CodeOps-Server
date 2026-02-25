package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to send a message in a direct conversation. */
public record SendDirectMessageRequest(
        @NotBlank @Size(max = 10000) String content
) {}
