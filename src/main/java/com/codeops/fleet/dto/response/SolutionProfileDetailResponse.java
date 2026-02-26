package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full solution profile detail response including services. */
public record SolutionProfileDetailResponse(
        UUID id,
        String name,
        String description,
        Boolean isDefault,
        UUID teamId,
        List<SolutionServiceResponse> services,
        Instant createdAt,
        Instant updatedAt
) {}
