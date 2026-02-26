package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a network configuration for a service profile. */
public record CreateNetworkConfigRequest(
        @NotBlank @Size(max = 200) String networkName,
        String aliases,
        @Size(max = 45) String ipAddress
) {}
