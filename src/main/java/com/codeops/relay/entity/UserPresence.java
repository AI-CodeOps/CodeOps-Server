package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.PresenceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks a user's online presence within a team.
 *
 * <p>Presence is updated via WebSocket heartbeats or HTTP polls. Users
 * whose heartbeat exceeds the timeout threshold are automatically
 * transitioned to OFFLINE.</p>
 */
@Entity
@Table(name = "user_presences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_presence_user_team",
                columnNames = {"user_id", "team_id"}),
        indexes = {
                @Index(name = "idx_presence_team_id", columnList = "team_id"),
                @Index(name = "idx_presence_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresence extends BaseEntity {

    /** User whose presence is tracked. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Team context for this presence record. */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /** Current online status of the user. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PresenceStatus status = PresenceStatus.OFFLINE;

    /** When the user was last seen active. */
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /** Last heartbeat timestamp from WebSocket ping or HTTP poll. */
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    /** Custom status text set by the user (e.g., "In a meeting"). */
    @Column(name = "status_message", length = 200)
    private String statusMessage;
}
