package com.codeops.relay.dto.request;

import jakarta.validation.constraints.Size;

/** Request to update an existing channel's name, description, or archive status. */
public record UpdateChannelRequest(
        @Size(max = 100) String name,
        String description,
        Boolean isArchived
) {}
