package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a message pinned in a channel.
 *
 * <p>Only one pin per message per channel is allowed. Pinned messages
 * appear in a dedicated panel for quick reference.</p>
 */
@Entity
@Table(name = "pinned_messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pinned_message",
                columnNames = {"channel_id", "message_id"}),
        indexes = {
                @Index(name = "idx_pinned_message_channel_id", columnList = "channel_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedMessage extends BaseEntity {

    /** The message that is pinned. */
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    /** User who pinned this message. */
    @Column(name = "pinned_by", nullable = false)
    private UUID pinnedBy;

    /** The channel this pin belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;
}
