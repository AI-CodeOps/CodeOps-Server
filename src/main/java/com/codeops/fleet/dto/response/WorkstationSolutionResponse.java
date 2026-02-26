package com.codeops.fleet.dto.response;

import java.util.UUID;

/** Response for a solution within a workstation profile. */
public record WorkstationSolutionResponse(
        UUID id,
        Integer startOrder,
        String overrideEnvVarsJson,
        UUID solutionProfileId,
        String solutionProfileName
) {}
