package com.codeops.relay.dto.request;

import java.util.UUID;

/** Request payload for broadcasting a typing indicator in a direct conversation. */
public record DmTypingRequest(UUID conversationId) {}
