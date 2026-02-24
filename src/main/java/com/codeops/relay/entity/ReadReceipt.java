package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the most recent message a user has read in a channel.
 *
 * <p>Used to compute unread message counts and render read indicators.
 * Each user has at most one receipt per channel.</p>
 */
@Entity
@Table(name = "read_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_read_receipt",
                columnNames = {"channel_id", "user_id"}),
        indexes = {
                @Index(name = "idx_read_receipt_channel_id", columnList = "channel_id"),
                @Index(name = "idx_read_receipt_user_id", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadReceipt extends BaseEntity {

    /** Channel this receipt applies to. */
    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    /** User who read the messages. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** ID of the most recent message the user has read. */
    @Column(name = "last_read_message_id", nullable = false)
    private UUID lastReadMessageId;

    /** When the user last read messages in this channel. */
    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;
}
