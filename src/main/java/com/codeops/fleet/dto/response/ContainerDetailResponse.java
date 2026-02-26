package com.codeops.fleet.dto.response;

import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;

import java.time.Instant;
import java.util.UUID;

/** Full container detail response including relationships. */
public record ContainerDetailResponse(
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
        Integer exitCode,
        Double cpuPercent,
        Long memoryBytes,
        Long memoryLimitBytes,
        Integer pid,
        Instant startedAt,
        Instant finishedAt,
        UUID serviceProfileId,
        String serviceProfileName,
        UUID teamId,
        Instant createdAt,
        Instant updatedAt
) {}
