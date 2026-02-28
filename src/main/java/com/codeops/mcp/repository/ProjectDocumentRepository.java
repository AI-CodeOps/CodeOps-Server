package com.codeops.mcp.repository;

import com.codeops.mcp.entity.ProjectDocument;
import com.codeops.mcp.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProjectDocument} entities.
 *
 * <p>Provides CRUD operations plus queries for listing documents
 * by project, looking up by type (and optional custom name),
 * finding flagged documents, and checking existence.</p>
 */
@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    /**
     * Lists all documents for a project.
     *
     * @param projectId the project ID
     * @return list of documents
     */
    List<ProjectDocument> findByProjectId(UUID projectId);

    /**
     * Finds a document by project and document type.
     *
     * @param projectId    the project ID
     * @param documentType the document type
     * @return the document if found
     */
    Optional<ProjectDocument> findByProjectIdAndDocumentType(UUID projectId, DocumentType documentType);

    /**
     * Finds a document by project, type, and custom name (for CUSTOM types).
     *
     * @param projectId    the project ID
     * @param documentType the document type
     * @param customName   the custom document name
     * @return the document if found
     */
    Optional<ProjectDocument> findByProjectIdAndDocumentTypeAndCustomName(UUID projectId, DocumentType documentType, String customName);

    /**
     * Lists all flagged documents for a project.
     *
     * @param projectId the project ID
     * @return list of flagged documents
     */
    List<ProjectDocument> findByProjectIdAndIsFlaggedTrue(UUID projectId);

    /**
     * Checks whether a document of a given type exists for a project.
     *
     * @param projectId    the project ID
     * @param documentType the document type
     * @return true if a document exists
     */
    boolean existsByProjectIdAndDocumentType(UUID projectId, DocumentType documentType);
}
