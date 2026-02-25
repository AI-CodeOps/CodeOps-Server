package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full message response with reactions, attachments, and thread metadata. */
public record MessageResponse(
        UUID id,
        UUID channelId,
        UUID senderId,
        String senderDisplayName,
        String content,
        MessageType messageType,
        UUID parentId,
        boolean isEdited,
        Instant editedAt,
        boolean isDeleted,
        boolean mentionsEveryone,
        List<UUID> mentionedUserIds,
        UUID platformEventId,
        List<ReactionSummaryResponse> reactions,
        List<FileAttachmentResponse> attachments,
        int replyCount,
        Instant lastReplyAt,
        Instant createdAt,
        Instant updatedAt
) {}
