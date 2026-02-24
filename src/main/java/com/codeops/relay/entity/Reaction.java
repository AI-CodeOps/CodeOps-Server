package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.ReactionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents an emoji reaction on a channel message or direct message.
 *
 * <p>A reaction is linked to either a {@link Message} (channel) or a
 * {@link DirectMessage} (DM), but not both. Each user can add the same
 * emoji only once per message.</p>
 */
@Entity
@Table(name = "reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reaction_message",
                        columnNames = {"message_id", "user_id", "emoji"}),
                @UniqueConstraint(
                        name = "uk_reaction_dm",
                        columnNames = {"direct_message_id", "user_id", "emoji"})
        },
        indexes = {
                @Index(name = "idx_reaction_message_id", columnList = "message_id"),
                @Index(name = "idx_reaction_dm_id", columnList = "direct_message_id"),
                @Index(name = "idx_reaction_user_id", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction extends BaseEntity {

    /** User who added this reaction. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Unicode emoji character (e.g., "\uD83D\uDC4D", "\uD83C\uDF89"). */
    @Column(nullable = false, length = 50)
    private String emoji;

    /** Classification of this reaction. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private ReactionType reactionType = ReactionType.EMOJI;

    /** Channel message this reaction is on (null if on a direct message). */
    @Column(name = "message_id")
    private UUID messageId;

    /** Direct message this reaction is on (null if on a channel message). */
    @Column(name = "direct_message_id")
    private UUID directMessageId;

    /** The channel message entity (read-only relationship). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private Message message;

    /** The direct message entity (read-only relationship). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direct_message_id", insertable = false, updatable = false)
    private DirectMessage directMessage;
}
