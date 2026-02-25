package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.FileUploadStatus;

import java.time.Instant;
import java.util.UUID;

/** File attachment detail with download URLs. */
public record FileAttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String downloadUrl,
        String thumbnailUrl,
        FileUploadStatus status,
        UUID uploadedBy,
        Instant createdAt
) {}
