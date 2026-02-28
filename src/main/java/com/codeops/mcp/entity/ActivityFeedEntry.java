package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.entity.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entry in the MCP activity feed for a team.
 *
 * <p>Provides visibility into AI agent operations including session
 * completions, document updates, convention changes, and cross-project
 * impact detection. Entries are team-scoped and ordered by creation time.</p>
 */
@Entity
@Table(name = "mcp_activity_feed",
        indexes = {
                @Index(name = "idx_mcp_af_team_created",
                        columnList = "team_id, created_at DESC"),
                @Index(name = "idx_mcp_af_project_created",
                        columnList = "project_id, created_at DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedEntry extends BaseEntity {

    /** Classification of this activity entry. */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    /** Human-readable title for the activity. */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /** Full detail or summary of the activity. */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** CodeOps module that generated this entry (e.g., "mcp", "registry", "fleet"). */
    @Column(name = "source_module", length = 100)
    private String sourceModule;

    /** ID of the source entity within the originating module. */
    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    /** Denormalized project name for display without joining. */
    @Column(name = "project_name", length = 200)
    private String projectName;

    /** JSON array of UUIDs for services impacted by this activity. */
    @Column(name = "impacted_service_ids_json", columnDefinition = "TEXT")
    private String impactedServiceIdsJson;

    /** ID of the Relay message if this activity was posted to a channel. */
    @Column(name = "relay_message_id")
    private UUID relayMessageId;

    /** Team this activity entry belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** User who performed the action (null for system-generated entries). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Project associated with this activity. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** MCP session associated with this activity. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private McpSession session;
}
