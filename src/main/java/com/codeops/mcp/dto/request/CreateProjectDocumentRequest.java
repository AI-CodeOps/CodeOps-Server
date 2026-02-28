package com.codeops.mcp.dto.request;

import com.codeops.mcp.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new project document managed by MCP.
 *
 * @param documentType      classification of the document
 * @param customName        name for CUSTOM document types; required if documentType is CUSTOM
 * @param content           initial document content
 * @param changeDescription description of what this document contains
 * @param commitHash        Git commit hash associated with the creation
 */
public record CreateProjectDocumentRequest(
        @NotNull DocumentType documentType,
        @Size(max = 200) String customName,
        @NotBlank String content,
        @Size(max = 500) String changeDescription,
        @Size(max = 100) String commitHash
) {}
