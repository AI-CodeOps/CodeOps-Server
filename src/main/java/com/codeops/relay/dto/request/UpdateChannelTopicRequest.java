package com.codeops.relay.dto.request;

import jakarta.validation.constraints.Size;

/** Request to update a channel's topic. */
public record UpdateChannelTopicRequest(
        @Size(max = 500) String topic
) {}
