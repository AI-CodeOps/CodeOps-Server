package com.codeops.fleet.dto.request;

import com.codeops.fleet.entity.enums.RestartPolicy;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/** Request to start a container from a service profile. */
public record StartContainerRequest(
        @NotNull UUID serviceProfileId,
        String containerNameOverride,
        String imageTagOverride,
        Map<String, String> envVarOverrides,
        RestartPolicy restartPolicyOverride
) {}
