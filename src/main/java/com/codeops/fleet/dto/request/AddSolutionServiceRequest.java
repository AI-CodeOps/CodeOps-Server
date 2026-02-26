package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request to add a service profile to a solution profile. */
public record AddSolutionServiceRequest(
        @NotNull UUID serviceProfileId,
        Integer startOrder
) {}
