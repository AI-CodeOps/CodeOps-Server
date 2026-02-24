package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.ChannelType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a messaging channel within a team.
 *
 * <p>Channels can be public (anyone joins), private (invite-only),
 * project-linked (auto-created per project), or service-linked
 * (auto-created per registered service).</p>
 */
@Entity
@Table(name = "channels",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_team_slug",
                columnNames = {"team_id", "slug"}),
        indexes = {
                @Index(name = "idx_channel_team_id", columnList = "team_id"),
                @Index(name = "idx_channel_type", columnList = "channel_type"),
                @Index(name = "idx_channel_project_id", columnList = "project_id"),
                @Index(name = "idx_channel_service_id", columnList = "service_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel extends BaseEntity {

    /** Display name of the channel (e.g., "General", "Project API Gateway"). */
    @Column(nullable = false, length = 100)
    private String name;

    /** URL-safe lowercase slug (e.g., "general", "project-api-gateway"). */
    @Column(nullable = false, length = 100)
    private String slug;

    /** Optional long-form description of the channel's purpose. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Current channel topic, displayed in the channel header. */
    @Column(length = 500)
    private String topic;

    /** Classification of this channel. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    /** Team that owns this channel. */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /** Associated project ID for PROJECT channels (no JPA relation to avoid cross-module coupling). */
    @Column(name = "project_id")
    private UUID projectId;

    /** Associated service ID for SERVICE channels (FK reference to service_registrations). */
    @Column(name = "service_id")
    private UUID serviceId;

    /** Whether this channel has been archived. Archived channels are read-only. */
    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    /** User who created this channel. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /** Members of this channel. */
    @Builder.Default
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelMember> members = new ArrayList<>();

    /** Messages pinned in this channel. */
    @Builder.Default
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PinnedMessage> pinnedMessages = new ArrayList<>();
}
