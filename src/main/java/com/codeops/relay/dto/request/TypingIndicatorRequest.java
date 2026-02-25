package com.codeops.relay.dto.request;

import java.util.UUID;

/** Request payload for broadcasting a typing indicator in a channel. */
public record TypingIndicatorRequest(UUID channelId) {}
