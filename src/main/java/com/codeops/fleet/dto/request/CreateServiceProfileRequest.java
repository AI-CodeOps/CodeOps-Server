package com.codeops.fleet.dto.request;

import com.codeops.fleet.entity.enums.RestartPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request to create a new service profile for Docker container configuration. */
public record CreateServiceProfileRequest(
        @NotBlank @Size(max = 200) String serviceName,
        @Size(max = 200) String displayName,
        String description,
        @NotBlank @Size(max = 500) String imageName,
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
        Integer startOrder,
        UUID serviceRegistrationId
) {}
