package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Container log entry response. */
public record ContainerLogResponse(
        UUID id,
        String stream,
        String content,
        Instant timestamp,
        UUID containerId,
        Instant createdAt
) {}
