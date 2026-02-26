package com.codeops.fleet.dto.response;

import java.util.UUID;

/** Network configuration response. */
public record NetworkConfigResponse(
        UUID id,
        String networkName,
        String aliases,
        String ipAddress
) {}
