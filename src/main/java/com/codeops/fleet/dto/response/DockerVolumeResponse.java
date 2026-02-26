package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.Map;

/** Docker volume metadata response. */
public record DockerVolumeResponse(
        String name,
        String driver,
        String mountpoint,
        Map<String, String> labels,
        Instant createdAt
) {}
