package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.AuthorType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a specific version of a project document.
 *
 * @param id                version ID
 * @param versionNumber     sequential version number
 * @param content           full content at this version
 * @param authorType        who created this version (HUMAN or AI)
 * @param commitHash        associated Git commit hash
 * @param changeDescription what changed in this version
 * @param authorName        display name of the author
 * @param sessionId         MCP session that produced this version
 * @param createdAt         creation timestamp
 */
public record ProjectDocumentVersionResponse(
        UUID id,
        int versionNumber,
        String content,
        AuthorType authorType,
        String commitHash,
        String changeDescription,
        String authorName,
        UUID sessionId,
        Instant createdAt
) {}
