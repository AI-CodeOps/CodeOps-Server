package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request to add a solution profile to a workstation profile. */
public record AddWorkstationSolutionRequest(
        @NotNull UUID solutionProfileId,
        Integer startOrder,
        String overrideEnvVarsJson
) {}
