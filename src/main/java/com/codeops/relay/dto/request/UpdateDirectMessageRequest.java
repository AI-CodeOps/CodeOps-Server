package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to edit a previously sent direct message. */
public record UpdateDirectMessageRequest(
        @NotBlank @Size(max = 10000) String content
) {}
