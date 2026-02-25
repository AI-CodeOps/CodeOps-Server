package com.codeops.relay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Request to send a message in a channel. */
public record SendMessageRequest(
        @NotBlank @Size(max = 10000) String content,
        UUID parentId,
        List<UUID> mentionedUserIds,
        Boolean mentionsEveryone
) {}
