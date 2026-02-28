package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.User;
import com.codeops.mcp.entity.enums.AuthorType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a historical version of a project document.
 *
 * <p>Each time a document is updated, a new version is created
 * with the full content at that point in time. Versions are
 * numbered sequentially per document.</p>
 */
@Entity
@Table(name = "mcp_project_document_versions",
        indexes = {
                @Index(name = "idx_mcp_pdv_doc_version",
                        columnList = "document_id, version_number DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDocumentVersion extends BaseEntity {

    /** Sequential version number within the parent document. */
    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    /** Full document content at this version. */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Whether this version was created by a human or AI agent. */
    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false)
    private AuthorType authorType;

    /** Git commit hash associated with this version change. */
    @Column(name = "commit_hash", length = 100)
    private String commitHash;

    /** Human-readable description of what changed in this version. */
    @Column(name = "change_description", length = 500)
    private String changeDescription;

    /** Parent document this version belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private ProjectDocument document;

    /** User who created this version (null for system updates). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    /** MCP session that produced this version (null for manual updates). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private McpSession session;
}
