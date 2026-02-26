package com.codeops.fleet.dto.response;

import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;

import java.time.Instant;
import java.util.UUID;

/** Lightweight container instance response for list views. */
public record ContainerInstanceResponse(
        UUID id,
        String containerId,
        String containerName,
        String serviceName,
        String imageName,
        String imageTag,
        ContainerStatus status,
        HealthStatus healthStatus,
        RestartPolicy restartPolicy,
        Integer restartCount,
        Double cpuPercent,
        Long memoryBytes,
        Long memoryLimitBytes,
        Instant startedAt,
        Instant createdAt
) {}
