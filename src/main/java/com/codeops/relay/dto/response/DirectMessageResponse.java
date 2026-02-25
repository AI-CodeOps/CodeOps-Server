package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full direct message response with reactions and attachments. */
public record DirectMessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderDisplayName,
        String content,
        MessageType messageType,
        boolean isEdited,
        Instant editedAt,
        boolean isDeleted,
        List<ReactionSummaryResponse> reactions,
        List<FileAttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {}
