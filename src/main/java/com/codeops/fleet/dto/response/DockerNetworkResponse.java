package com.codeops.fleet.dto.response;

import java.util.List;

/** Docker network metadata response. */
public record DockerNetworkResponse(
        String id,
        String name,
        String driver,
        String subnet,
        String gateway,
        List<String> connectedContainers
) {}
