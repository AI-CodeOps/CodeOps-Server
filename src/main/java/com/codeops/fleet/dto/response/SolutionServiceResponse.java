package com.codeops.fleet.dto.response;

import java.util.UUID;

/** Response for a service within a solution profile. */
public record SolutionServiceResponse(
        UUID id,
        Integer startOrder,
        UUID serviceProfileId,
        String serviceProfileName,
        String imageName,
        Boolean isEnabled
) {}
