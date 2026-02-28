package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Project;
import com.codeops.entity.User;
import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a project document managed by MCP.
 *
 * <p>Stores the current content of documents like CLAUDE.md,
 * CONVENTIONS.md, architecture specs, and audits. Tracks
 * who last updated the document and maintains a version history.</p>
 */
@Entity
@Table(name = "mcp_project_documents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mcp_pd_project_type_name",
                columnNames = {"project_id", "document_type", "custom_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDocument extends BaseEntity {

    /** Classification of this document. */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    /** Name for CUSTOM document types; null for standard types. */
    @Column(name = "custom_name", length = 200)
    private String customName;

    /** Current full content of the document. */
    @Column(name = "current_content", columnDefinition = "TEXT")
    private String currentContent;

    /** Whether the last update was by a human or AI agent. */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_author_type")
    private AuthorType lastAuthorType;

    /** ID of the MCP session that last updated this document. */
    @Column(name = "last_session_id")
    private UUID lastSessionId;

    /** Whether this document has been flagged as potentially stale. */
    @Builder.Default
    @Column(name = "is_flagged", nullable = false)
    private boolean isFlagged = false;

    /** Reason for the staleness flag (e.g., "3 sessions since last update"). */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    /** Project this document belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** User who last updated this document. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by")
    private User lastUpdatedBy;

    /** Version history for this document. */
    @Builder.Default
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectDocumentVersion> versions = new ArrayList<>();
}
