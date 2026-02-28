package com.codeops.mcp.dto.request;

import com.codeops.mcp.entity.enums.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update an existing project document managed by MCP.
 *
 * @param content           new document content
 * @param changeDescription description of what changed
 * @param commitHash        Git commit hash associated with the update
 * @param authorType        who made the update; defaults to HUMAN
 */
public record UpdateProjectDocumentRequest(
        @NotBlank String content,
        @Size(max = 500) String changeDescription,
        @Size(max = 100) String commitHash,
        AuthorType authorType
) {}
