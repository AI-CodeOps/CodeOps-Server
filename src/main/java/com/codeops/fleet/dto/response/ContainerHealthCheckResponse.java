package com.codeops.fleet.dto.response;

import com.codeops.fleet.entity.enums.HealthStatus;

import java.time.Instant;
import java.util.UUID;

/** Health check result response for a container instance. */
public record ContainerHealthCheckResponse(
        UUID id,
        HealthStatus status,
        String output,
        Integer exitCode,
        Long durationMs,
        UUID containerId,
        Instant createdAt
) {}
