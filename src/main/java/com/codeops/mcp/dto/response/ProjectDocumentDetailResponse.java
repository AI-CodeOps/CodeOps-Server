package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response DTO for a project document including content and version history.
 *
 * @param id                document ID
 * @param documentType      classification of the document
 * @param customName        name for CUSTOM types
 * @param currentContent    full document content
 * @param lastAuthorType    who last updated (HUMAN or AI)
 * @param lastSessionId     session that last updated
 * @param isFlagged         whether flagged as stale
 * @param flagReason        reason for the flag
 * @param projectId         owning project ID
 * @param lastUpdatedByName name of the user who last updated
 * @param versions          version history
 * @param createdAt         creation timestamp
 * @param updatedAt         last update timestamp
 */
public record ProjectDocumentDetailResponse(
        UUID id,
        DocumentType documentType,
        String customName,
        String currentContent,
        AuthorType lastAuthorType,
        UUID lastSessionId,
        boolean isFlagged,
        String flagReason,
        UUID projectId,
        String lastUpdatedByName,
        List<ProjectDocumentVersionResponse> versions,
        Instant createdAt,
        Instant updatedAt
) {}
