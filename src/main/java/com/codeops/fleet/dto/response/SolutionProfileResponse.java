package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Lightweight solution profile response for list views. */
public record SolutionProfileResponse(
        UUID id,
        String name,
        String description,
        Boolean isDefault,
        int serviceCount,
        UUID teamId,
        Instant createdAt
) {}
