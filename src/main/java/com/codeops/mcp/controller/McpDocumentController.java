package com.codeops.mcp.controller;

import com.codeops.config.AppConstants;
import com.codeops.mcp.dto.request.CreateProjectDocumentRequest;
import com.codeops.mcp.dto.request.UpdateProjectDocumentRequest;
import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.ProjectDocumentVersionResponse;
import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.service.DocumentManagementService;
import com.codeops.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for MCP project document management.
 *
 * <p>Provides endpoints for creating, reading, updating, and deleting
 * versioned project documents (CLAUDE.md, CONVENTIONS.md, Architecture.md,
 * Audit.md, OpenAPI.yaml, and custom documents). Supports version history
 * retrieval and staleness flag management.</p>
 *
 * <p>All endpoints require ADMIN or OWNER role.</p>
 */
@RestController
@RequestMapping(AppConstants.MCP_API_PREFIX + "/documents")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class McpDocumentController {

    private final DocumentManagementService documentManagementService;

    /**
     * Creates a new project document with version 1.
     *
     * @param projectId the project ID
     * @param request   the document creation request
     * @return the created document detail with version 1
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDocumentDetailResponse createDocument(@RequestParam UUID projectId,
                                                          @RequestBody @Valid CreateProjectDocumentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return documentManagementService.createDocument(projectId, request, userId, AuthorType.HUMAN);
    }

    /**
     * Lists all documents for a project.
     *
     * @param projectId the project ID
     * @return list of document responses
     */
    @GetMapping
    public List<ProjectDocumentResponse> getProjectDocuments(@RequestParam UUID projectId) {
        return documentManagementService.getProjectDocuments(projectId);
    }

    /**
     * Gets a specific document by project and document type.
     *
     * @param projectId    the project ID
     * @param documentType the document type
     * @return the detailed document response
     */
    @GetMapping("/by-type")
    public ProjectDocumentDetailResponse getDocumentByType(@RequestParam UUID projectId,
                                                             @RequestParam DocumentType documentType) {
        return documentManagementService.getDocument(projectId, documentType);
    }

    /**
     * Updates a document's content and creates a new version.
     *
     * @param documentId the document ID to update
     * @param request    the update request with new content
     * @param sessionId  optional MCP session ID for AI-authored updates
     * @return the updated document detail
     */
    @PutMapping("/{documentId}")
    public ProjectDocumentDetailResponse updateDocument(@PathVariable UUID documentId,
                                                          @RequestBody @Valid UpdateProjectDocumentRequest request,
                                                          @RequestParam(required = false) UUID sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return documentManagementService.updateDocument(documentId, request, userId, sessionId);
    }

    /**
     * Retrieves paginated version history for a document.
     *
     * @param documentId the document ID
     * @param pageable   pagination parameters
     * @return page of version responses
     */
    @GetMapping("/{documentId}/versions")
    public Page<ProjectDocumentVersionResponse> getDocumentVersions(@PathVariable UUID documentId,
                                                                      Pageable pageable) {
        return documentManagementService.getDocumentVersions(documentId, pageable);
    }

    /**
     * Retrieves a specific version of a document by version number.
     *
     * @param documentId    the document ID
     * @param versionNumber the version number
     * @return the version response
     */
    @GetMapping("/{documentId}/versions/{versionNumber}")
    public ProjectDocumentVersionResponse getDocumentVersion(@PathVariable UUID documentId,
                                                               @PathVariable int versionNumber) {
        return documentManagementService.getDocumentVersion(documentId, versionNumber);
    }

    /**
     * Deletes a document and all its versions.
     *
     * @param documentId the document ID to delete
     */
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID documentId) {
        documentManagementService.deleteDocument(documentId);
    }

    /**
     * Gets all flagged (stale) documents for a project.
     *
     * @param projectId the project ID
     * @return list of flagged document responses
     */
    @GetMapping("/flagged")
    public List<ProjectDocumentResponse> getFlaggedDocuments(@RequestParam UUID projectId) {
        return documentManagementService.getFlaggedDocuments(projectId);
    }

    /**
     * Clears the staleness flag on a document.
     *
     * @param documentId the document ID to unflag
     */
    @PostMapping("/{documentId}/clear-flag")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearFlag(@PathVariable UUID documentId) {
        documentManagementService.clearFlag(documentId);
    }
}
