package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Real-time container resource statistics response. */
public record ContainerStatsResponse(
        UUID containerId,
        String containerName,
        Double cpuPercent,
        Long memoryUsageBytes,
        Long memoryLimitBytes,
        Long networkRxBytes,
        Long networkTxBytes,
        Long blockReadBytes,
        Long blockWriteBytes,
        Integer pids,
        Instant timestamp
) {}
