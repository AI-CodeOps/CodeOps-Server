package com.codeops.relay.dto.request;

import com.codeops.relay.entity.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request to create a new messaging channel. */
public record CreateChannelRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull ChannelType channelType,
        @Size(max = 500) String topic
) {}
