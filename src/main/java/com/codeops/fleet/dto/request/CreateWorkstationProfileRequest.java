package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a new workstation profile for local development environments. */
public record CreateWorkstationProfileRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        Boolean isDefault
) {}
