package com.codeops.relay.dto.request;

import java.util.UUID;

/** Request payload for a WebSocket presence heartbeat. */
public record HeartbeatRequest(UUID teamId) {}
