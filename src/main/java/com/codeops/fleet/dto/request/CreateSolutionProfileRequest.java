package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a new solution profile grouping service profiles. */
public record CreateSolutionProfileRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        Boolean isDefault
) {}
