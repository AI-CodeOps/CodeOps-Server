package com.codeops.fleet.dto.request;

import com.codeops.fleet.entity.enums.RestartPolicy;
import jakarta.validation.constraints.Size;

/** Request to update an existing service profile. */
public record UpdateServiceProfileRequest(
        @Size(max = 200) String displayName,
        String description,
        @Size(max = 500) String imageName,
        @Size(max = 100) String imageTag,
        String command,
        @Size(max = 500) String workingDir,
        String envVarsJson,
        String portsJson,
        String healthCheckCommand,
        Integer healthCheckIntervalSeconds,
        Integer healthCheckTimeoutSeconds,
        Integer healthCheckRetries,
        RestartPolicy restartPolicy,
        Integer memoryLimitMb,
        Double cpuLimit,
        Boolean isEnabled,
        Integer startOrder
) {}
