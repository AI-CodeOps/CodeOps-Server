package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user's membership in a channel.
 *
 * <p>Tracks the user's role, mute preference, and last-read position
 * for computing unread message counts.</p>
 */
@Entity
@Table(name = "channel_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_member",
                columnNames = {"channel_id", "user_id"}),
        indexes = {
                @Index(name = "idx_channel_member_user_id", columnList = "user_id"),
                @Index(name = "idx_channel_member_channel_id", columnList = "channel_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelMember extends BaseEntity {

    /** Channel this membership belongs to (denormalized for direct queries). */
    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    /** User who is a member of the channel. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Role of this member within the channel. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.MEMBER;

    /** Timestamp of the last message this user has read in the channel. */
    @Column(name = "last_read_at")
    private Instant lastReadAt;

    /** Whether notifications from this channel are suppressed for this user. */
    @Builder.Default
    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;

    /** When this user joined the channel. */
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    /** The channel entity (read-only JPA relationship). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", insertable = false, updatable = false)
    private Channel channel;
}
