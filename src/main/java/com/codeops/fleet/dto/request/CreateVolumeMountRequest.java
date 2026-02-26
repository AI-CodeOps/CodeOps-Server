package com.codeops.fleet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a volume mount for a service profile. */
public record CreateVolumeMountRequest(
        @Size(max = 500) String hostPath,
        @NotBlank @Size(max = 500) String containerPath,
        @Size(max = 200) String volumeName,
        Boolean isReadOnly
) {}
