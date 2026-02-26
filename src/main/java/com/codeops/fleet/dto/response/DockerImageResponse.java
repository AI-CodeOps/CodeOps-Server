package com.codeops.fleet.dto.response;

import java.time.Instant;
import java.util.List;

/** Docker image metadata response. */
public record DockerImageResponse(
        String id,
        List<String> repoTags,
        Long sizeBytes,
        Instant created
) {}
