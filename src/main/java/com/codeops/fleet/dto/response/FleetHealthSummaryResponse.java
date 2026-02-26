package com.codeops.fleet.dto.response;

import java.time.Instant;

/** Aggregated fleet health summary across all containers in a team. */
public record FleetHealthSummaryResponse(
        int totalContainers,
        int runningContainers,
        int stoppedContainers,
        int unhealthyContainers,
        int restartingContainers,
        double totalCpuPercent,
        long totalMemoryBytes,
        long totalMemoryLimitBytes,
        Instant timestamp
) {}
