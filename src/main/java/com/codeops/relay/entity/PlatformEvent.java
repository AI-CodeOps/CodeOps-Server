package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.PlatformEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a cross-module event posted to a Relay channel.
 *
 * <p>Platform events originate from other CodeOps modules (core, registry,
 * logger, courier) and are automatically formatted and posted to the
 * appropriate channel. Delivery tracking ensures no events are lost.</p>
 */
@Entity
@Table(name = "platform_events",
        indexes = {
                @Index(name = "idx_platform_event_team_id", columnList = "team_id"),
                @Index(name = "idx_platform_event_type", columnList = "event_type"),
                @Index(name = "idx_platform_event_created_at", columnList = "created_at"),
                @Index(name = "idx_platform_event_source_module", columnList = "source_module")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformEvent extends BaseEntity {

    /** Type of platform event. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PlatformEventType eventType;

    /** Team this event belongs to. */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /** Module that generated this event ("core", "registry", "logger", "courier"). */
    @Column(name = "source_module", nullable = false, length = 50)
    private String sourceModule;

    /** ID of the entity that triggered the event (jobId, alertId, serviceId, etc.). */
    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    /** Formatted event title for display. */
    @Column(nullable = false, length = 500)
    private String title;

    /** Formatted event detail/body for display. */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** Channel where this event was posted. */
    @Column(name = "target_channel_id")
    private UUID targetChannelId;

    /** Slug of the target channel (for audit/debugging). */
    @Column(name = "target_channel_slug", length = 100)
    private String targetChannelSlug;

    /** Message ID created from this event. */
    @Column(name = "posted_message_id")
    private UUID postedMessageId;

    /** Whether this event has been successfully posted as a message. */
    @Builder.Default
    @Column(name = "is_delivered", nullable = false)
    private boolean isDelivered = false;

    /** When the event was delivered as a message. */
    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
