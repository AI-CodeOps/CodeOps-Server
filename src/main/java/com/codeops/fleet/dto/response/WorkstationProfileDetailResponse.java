package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full workstation profile detail response including solutions. */
public record WorkstationProfileDetailResponse(
        UUID id,
        String name,
        String description,
        Boolean isDefault,
        UUID userId,
        UUID teamId,
        List<WorkstationSolutionResponse> solutions,
        Instant createdAt,
        Instant updatedAt
) {}
