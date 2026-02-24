package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a direct message conversation between two or more users.
 *
 * <p>ONE_ON_ONE conversations are between exactly two users; GROUP
 * conversations support three or more participants. Participant IDs
 * are stored as a sorted comma-separated string to ensure uniqueness.</p>
 */
@Entity
@Table(name = "direct_conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dm_participants",
                columnNames = {"team_id", "participant_ids"}),
        indexes = {
                @Index(name = "idx_dm_team_id", columnList = "team_id"),
                @Index(name = "idx_dm_last_message_at", columnList = "last_message_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectConversation extends BaseEntity {

    /** Team this conversation belongs to. */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /** Whether this is a one-on-one or group conversation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    private ConversationType conversationType;

    /** Display name for group conversations (null for 1:1, derived from participants). */
    @Column(length = 200)
    private String name;

    /** Sorted comma-separated UUIDs of all participants. */
    @Column(name = "participant_ids", columnDefinition = "TEXT", nullable = false)
    private String participantIds;

    /** Timestamp of the most recent message in this conversation. */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    /** Truncated preview of the last message for list view rendering. */
    @Column(name = "last_message_preview", length = 500)
    private String lastMessagePreview;
}
