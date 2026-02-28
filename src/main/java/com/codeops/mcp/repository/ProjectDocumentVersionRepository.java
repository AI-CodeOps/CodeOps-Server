package com.codeops.mcp.repository;

import com.codeops.mcp.entity.ProjectDocumentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProjectDocumentVersion} entities.
 *
 * <p>Provides CRUD operations plus queries for listing versions
 * by document (newest first), looking up specific versions,
 * finding the latest version, and determining the max version number.</p>
 */
@Repository
public interface ProjectDocumentVersionRepository extends JpaRepository<ProjectDocumentVersion, UUID> {

    /**
     * Lists all versions for a document, newest first.
     *
     * @param documentId the document ID
     * @return list of versions ordered by version number descending
     */
    List<ProjectDocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    /**
     * Returns a page of versions for a document.
     *
     * @param documentId the document ID
     * @param pageable   pagination parameters
     * @return page of versions
     */
    Page<ProjectDocumentVersion> findByDocumentId(UUID documentId, Pageable pageable);

    /**
     * Finds a specific version by document and version number.
     *
     * @param documentId    the document ID
     * @param versionNumber the version number
     * @return the version if found
     */
    Optional<ProjectDocumentVersion> findByDocumentIdAndVersionNumber(UUID documentId, int versionNumber);

    /**
     * Finds the latest version for a document.
     *
     * @param documentId the document ID
     * @return the latest version if any exist
     */
    Optional<ProjectDocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    /**
     * Returns the maximum version number for a document.
     *
     * @param documentId the document ID
     * @return the max version number, or null if no versions exist
     */
    @Query("SELECT MAX(v.versionNumber) FROM ProjectDocumentVersion v WHERE v.document.id = :documentId")
    Integer findMaxVersionNumber(@Param("documentId") UUID documentId);
}
