package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Lightweight workstation profile response for list views. */
public record WorkstationProfileResponse(
        UUID id,
        String name,
        String description,
        Boolean isDefault,
        int solutionCount,
        UUID userId,
        UUID teamId,
        Instant createdAt
) {}
