package com.codeops.fleet.dto.response;

import java.util.UUID;

/** Volume mount configuration response. */
public record VolumeMountResponse(
        UUID id,
        String hostPath,
        String containerPath,
        String volumeName,
        Boolean isReadOnly
) {}
