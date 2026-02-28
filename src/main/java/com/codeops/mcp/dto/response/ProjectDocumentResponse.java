package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight list-view response DTO for a project document.
 *
 * @param id               document ID
 * @param documentType     classification of the document
 * @param customName       name for CUSTOM types
 * @param lastAuthorType   who last updated (HUMAN or AI)
 * @param lastSessionId    session that last updated
 * @param isFlagged        whether flagged as stale
 * @param flagReason       reason for the flag
 * @param projectId        owning project ID
 * @param lastUpdatedByName name of the user who last updated
 * @param createdAt        creation timestamp
 * @param updatedAt        last update timestamp
 */
public record ProjectDocumentResponse(
        UUID id,
        DocumentType documentType,
        String customName,
        AuthorType lastAuthorType,
        UUID lastSessionId,
        boolean isFlagged,
        String flagReason,
        UUID projectId,
        String lastUpdatedByName,
        Instant createdAt,
        Instant updatedAt
) {}
