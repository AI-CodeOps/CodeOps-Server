package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.Size;

/** Request to update an existing solution profile. */
public record UpdateSolutionProfileRequest(
        @Size(max = 200) String name,
        String description,
        Boolean isDefault
) {}
