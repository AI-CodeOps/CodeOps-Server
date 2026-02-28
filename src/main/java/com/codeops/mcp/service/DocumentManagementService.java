package com.codeops.mcp.service;

import com.codeops.entity.Project;
import com.codeops.entity.User;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.mcp.dto.mapper.ProjectDocumentMapper;
import com.codeops.mcp.dto.mapper.ProjectDocumentVersionMapper;
import com.codeops.mcp.dto.request.CompleteSessionRequest.DocumentUpdate;
import com.codeops.mcp.dto.request.CreateProjectDocumentRequest;
import com.codeops.mcp.dto.request.UpdateProjectDocumentRequest;
import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.ProjectDocumentVersionResponse;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.ProjectDocument;
import com.codeops.mcp.entity.ProjectDocumentVersion;
import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.repository.McpSessionRepository;
import com.codeops.mcp.repository.ProjectDocumentRepository;
import com.codeops.mcp.repository.ProjectDocumentVersionRepository;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages versioned project documents for MCP AI sessions.
 *
 * <p>Handles the full lifecycle of project documents (CLAUDE.md, CONVENTIONS.md,
 * Architecture.md, Audit.md, OpenAPI.yaml, and custom documents): creation,
 * versioned updates, session writeback, staleness detection, and retrieval.</p>
 *
 * <p>Every content update creates a new {@link ProjectDocumentVersion} with
 * auto-incremented version numbers. The parent document's {@code currentContent}
 * is always kept in sync for fast reads. Documents can be flagged as stale
 * when too many sessions complete without updating them.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentManagementService {

    private final ProjectDocumentRepository documentRepository;
    private final ProjectDocumentVersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final McpSessionRepository sessionRepository;
    private final ProjectDocumentMapper documentMapper;
    private final ProjectDocumentVersionMapper versionMapper;

    /** Default threshold of sessions before flagging documents as stale. */
    private static final int DEFAULT_STALENESS_THRESHOLD = 3;

    /**
     * Creates a new project document with version 1.
     *
     * <p>Validates that CUSTOM document types have a non-null customName,
     * and that no document of the same type (and customName) already exists
     * for the project.</p>
     *
     * @param projectId  the project to create the document for
     * @param request    the creation request with type, content, and metadata
     * @param userId     the user creating the document
     * @param authorType whether the creator is HUMAN or AI
     * @return the detailed document response with version 1
     * @throws NotFoundException   if the project or user is not found
     * @throws ValidationException if CUSTOM type lacks customName or document already exists
     */
    @Transactional
    public ProjectDocumentDetailResponse createDocument(UUID projectId,
                                                         CreateProjectDocumentRequest request,
                                                         UUID userId,
                                                         AuthorType authorType) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        // Validate CUSTOM type requires customName
        if (request.documentType() == DocumentType.CUSTOM
                && (request.customName() == null || request.customName().isBlank())) {
            throw new ValidationException("Custom document type requires a non-null customName");
        }

        // Check for duplicate
        if (request.documentType() == DocumentType.CUSTOM) {
            if (documentRepository.findByProjectIdAndDocumentTypeAndCustomName(
                    projectId, request.documentType(), request.customName()).isPresent()) {
                throw new ValidationException("Document of type " + request.documentType()
                        + " with name '" + request.customName() + "' already exists for this project");
            }
        } else {
            if (documentRepository.existsByProjectIdAndDocumentType(projectId, request.documentType())) {
                throw new ValidationException("Document of type " + request.documentType()
                        + " already exists for this project");
            }
        }

        ProjectDocument document = ProjectDocument.builder()
                .documentType(request.documentType())
                .customName(request.customName())
                .currentContent(request.content())
                .lastAuthorType(authorType)
                .project(project)
                .lastUpdatedBy(user)
                .build();

        document = documentRepository.save(document);

        ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                .versionNumber(1)
                .content(request.content())
                .authorType(authorType)
                .commitHash(request.commitHash())
                .changeDescription(request.changeDescription())
                .document(document)
                .author(user)
                .build();

        version = versionRepository.save(version);

        log.info("Created document {} (type: {}) for project {} with version 1",
                document.getId(), request.documentType(), projectId);

        List<ProjectDocumentVersionResponse> versions = List.of(versionMapper.toResponse(version));
        return documentMapper.toDetailResponse(document, versions);
    }

    /**
     * Updates a project document's content and creates a new version.
     *
     * <p>Increments the version number from the current maximum, updates
     * the parent document's currentContent, and tracks the author and
     * optional session ID.</p>
     *
     * @param documentId the document to update
     * @param request    the update request with new content and metadata
     * @param userId     the user performing the update
     * @param sessionId  the MCP session (null for manual updates)
     * @return the detailed document response with updated version list
     * @throws NotFoundException if the document or user is not found
     */
    @Transactional
    public ProjectDocumentDetailResponse updateDocument(UUID documentId,
                                                         UpdateProjectDocumentRequest request,
                                                         UUID userId,
                                                         UUID sessionId) {
        ProjectDocument document = findDocument(documentId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        AuthorType authorType = request.authorType() != null ? request.authorType() : AuthorType.HUMAN;

        Integer maxVersion = versionRepository.findMaxVersionNumber(documentId);
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        McpSession session = null;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId).orElse(null);
        }

        ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                .versionNumber(nextVersion)
                .content(request.content())
                .authorType(authorType)
                .commitHash(request.commitHash())
                .changeDescription(request.changeDescription())
                .document(document)
                .author(user)
                .session(session)
                .build();

        versionRepository.save(version);

        // Update parent document
        document.setCurrentContent(request.content());
        document.setLastAuthorType(authorType);
        document.setLastUpdatedBy(user);
        if (sessionId != null) {
            document.setLastSessionId(sessionId);
        }
        // Clear staleness flag on update
        document.setFlagged(false);
        document.setFlagReason(null);

        document = documentRepository.save(document);

        log.info("Updated document {} to version {} (author: {})",
                documentId, nextVersion, authorType);

        List<ProjectDocumentVersionResponse> versions = versionMapper.toResponseList(
                versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId));
        return documentMapper.toDetailResponse(document, versions);
    }

    /**
     * Gets all documents for a project.
     *
     * @param projectId the project ID
     * @return list of document responses
     */
    @Transactional(readOnly = true)
    public List<ProjectDocumentResponse> getProjectDocuments(UUID projectId) {
        List<ProjectDocument> documents = documentRepository.findByProjectId(projectId);
        return documentMapper.toResponseList(documents);
    }

    /**
     * Gets a specific document by project and document type.
     *
     * @param projectId    the project ID
     * @param documentType the document type
     * @return the detailed document response
     * @throws NotFoundException if no document of that type exists for the project
     */
    @Transactional(readOnly = true)
    public ProjectDocumentDetailResponse getDocument(UUID projectId, DocumentType documentType) {
        ProjectDocument document = documentRepository
                .findByProjectIdAndDocumentType(projectId, documentType)
                .orElseThrow(() -> new NotFoundException("ProjectDocument", "type",
                        documentType.name() + " for project " + projectId));

        List<ProjectDocumentVersionResponse> versions = versionMapper.toResponseList(
                versionRepository.findByDocumentIdOrderByVersionNumberDesc(document.getId()));
        return documentMapper.toDetailResponse(document, versions);
    }

    /**
     * Gets a custom document by project and custom name.
     *
     * @param projectId  the project ID
     * @param customName the custom document name
     * @return the detailed document response
     * @throws NotFoundException if no custom document with that name exists
     */
    @Transactional(readOnly = true)
    public ProjectDocumentDetailResponse getCustomDocument(UUID projectId, String customName) {
        ProjectDocument document = documentRepository
                .findByProjectIdAndDocumentTypeAndCustomName(projectId, DocumentType.CUSTOM, customName)
                .orElseThrow(() -> new NotFoundException("ProjectDocument", "customName",
                        customName + " for project " + projectId));

        List<ProjectDocumentVersionResponse> versions = versionMapper.toResponseList(
                versionRepository.findByDocumentIdOrderByVersionNumberDesc(document.getId()));
        return documentMapper.toDetailResponse(document, versions);
    }

    /**
     * Gets a specific version of a document by version number.
     *
     * @param documentId    the document ID
     * @param versionNumber the version number
     * @return the version response
     * @throws NotFoundException if the document or version is not found
     */
    @Transactional(readOnly = true)
    public ProjectDocumentVersionResponse getDocumentVersion(UUID documentId, int versionNumber) {
        findDocument(documentId); // verify document exists

        ProjectDocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new NotFoundException("ProjectDocumentVersion", "versionNumber",
                        String.valueOf(versionNumber)));

        return versionMapper.toResponse(version);
    }

    /**
     * Gets paginated version history for a document.
     *
     * @param documentId the document ID
     * @param pageable   pagination parameters
     * @return page of version responses
     * @throws NotFoundException if the document is not found
     */
    @Transactional(readOnly = true)
    public Page<ProjectDocumentVersionResponse> getDocumentVersions(UUID documentId, Pageable pageable) {
        findDocument(documentId); // verify document exists

        Page<ProjectDocumentVersion> versions = versionRepository.findByDocumentId(documentId, pageable);
        return versions.map(versionMapper::toResponse);
    }

    /**
     * Deletes a document and all its versions.
     *
     * <p>Uses cascade delete via the entity's orphanRemoval configuration.</p>
     *
     * @param documentId the document to delete
     * @throws NotFoundException if the document is not found
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        ProjectDocument document = findDocument(documentId);

        documentRepository.delete(document);
        log.info("Deleted document {} (type: {})", documentId, document.getDocumentType());
    }

    /**
     * Bulk-updates documents from an MCP session writeback.
     *
     * <p>Called by McpSessionService.completeSession(). For each document update,
     * either updates an existing document or creates a new one. Sets the author
     * type to AI and records the session ID.</p>
     *
     * @param projectId the project ID
     * @param sessionId the MCP session that produced the updates
     * @param userId    the user (developer profile's user) performing the updates
     * @param updates   the list of document updates to apply
     */
    @Transactional
    public void updateDocumentsFromSession(UUID projectId, UUID sessionId,
                                            UUID userId, List<DocumentUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        McpSession session = sessionRepository.findById(sessionId).orElse(null);

        for (DocumentUpdate update : updates) {
            ProjectDocument document = documentRepository
                    .findByProjectIdAndDocumentType(projectId, update.documentType())
                    .orElse(null);

            if (document != null) {
                // Update existing document
                Integer maxVersion = versionRepository.findMaxVersionNumber(document.getId());
                int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

                ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                        .versionNumber(nextVersion)
                        .content(update.content())
                        .authorType(AuthorType.AI)
                        .changeDescription(update.changeDescription())
                        .document(document)
                        .author(user)
                        .session(session)
                        .build();

                versionRepository.save(version);

                document.setCurrentContent(update.content());
                document.setLastAuthorType(AuthorType.AI);
                document.setLastSessionId(sessionId);
                document.setLastUpdatedBy(user);
                document.setFlagged(false);
                document.setFlagReason(null);
                documentRepository.save(document);

                log.debug("Session {} updated document {} to version {}",
                        sessionId, document.getId(), nextVersion);
            } else {
                // Create new document
                document = ProjectDocument.builder()
                        .documentType(update.documentType())
                        .currentContent(update.content())
                        .lastAuthorType(AuthorType.AI)
                        .lastSessionId(sessionId)
                        .project(project)
                        .lastUpdatedBy(user)
                        .build();

                document = documentRepository.save(document);

                ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                        .versionNumber(1)
                        .content(update.content())
                        .authorType(AuthorType.AI)
                        .changeDescription(update.changeDescription())
                        .document(document)
                        .author(user)
                        .session(session)
                        .build();

                versionRepository.save(version);

                log.debug("Session {} created new document {} (type: {})",
                        sessionId, document.getId(), update.documentType());
            }
        }

        log.info("Session {} applied {} document updates to project {}",
                sessionId, updates.size(), projectId);
    }

    /**
     * Checks and flags documents as stale based on sessions since last update.
     *
     * <p>If the number of completed sessions since a document's last update
     * exceeds the threshold, the document is flagged with a descriptive reason.</p>
     *
     * @param projectId          the project ID
     * @param sessionsSinceUpdate number of completed sessions since the document's last update
     */
    @Transactional
    public void checkAndFlagStaleness(UUID projectId, int sessionsSinceUpdate) {
        if (sessionsSinceUpdate < DEFAULT_STALENESS_THRESHOLD) {
            return;
        }

        List<ProjectDocument> documents = documentRepository.findByProjectId(projectId);

        for (ProjectDocument document : documents) {
            if (!document.isFlagged()) {
                document.setFlagged(true);
                document.setFlagReason(sessionsSinceUpdate
                        + " sessions completed since last update");
                documentRepository.save(document);
                log.info("Flagged document {} (type: {}) as stale: {} sessions since update",
                        document.getId(), document.getDocumentType(), sessionsSinceUpdate);
            }
        }
    }

    /**
     * Gets all flagged (stale) documents for a project.
     *
     * @param projectId the project ID
     * @return list of flagged document responses
     */
    @Transactional(readOnly = true)
    public List<ProjectDocumentResponse> getFlaggedDocuments(UUID projectId) {
        List<ProjectDocument> documents = documentRepository.findByProjectIdAndIsFlaggedTrue(projectId);
        return documentMapper.toResponseList(documents);
    }

    /**
     * Clears the staleness flag on a document.
     *
     * @param documentId the document to unflag
     * @throws NotFoundException if the document is not found
     */
    @Transactional
    public void clearFlag(UUID documentId) {
        ProjectDocument document = findDocument(documentId);

        document.setFlagged(false);
        document.setFlagReason(null);
        documentRepository.save(document);

        log.info("Cleared staleness flag on document {}", documentId);
    }

    /**
     * Gets all documents for a project as a map of document identifier to content.
     *
     * <p>Used by ContextAssemblyService for building the session context payload.
     * The key is the document type name, with custom documents using
     * {@code CUSTOM:<customName>} format.</p>
     *
     * @param projectId the project ID
     * @return map of document identifier to current content
     */
    @Transactional(readOnly = true)
    public Map<String, String> getDocumentsAsMap(UUID projectId) {
        List<ProjectDocument> documents = documentRepository.findByProjectId(projectId);
        Map<String, String> result = new LinkedHashMap<>();

        for (ProjectDocument doc : documents) {
            String key = doc.getDocumentType() == DocumentType.CUSTOM && doc.getCustomName() != null
                    ? doc.getDocumentType().name() + ":" + doc.getCustomName()
                    : doc.getDocumentType().name();
            if (doc.getCurrentContent() != null) {
                result.put(key, doc.getCurrentContent());
            }
        }

        return result;
    }

    // ── Private Helpers ──

    /**
     * Finds a document by ID or throws NotFoundException.
     */
    private ProjectDocument findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("ProjectDocument", documentId));
    }
}
