package com.codeops.relay.dto.request;

import java.util.UUID;

/** Metadata for a file upload. Actual file content arrives via MultipartFile. */
public record UploadFileRequest(
        UUID messageId,
        UUID directMessageId
) {}
