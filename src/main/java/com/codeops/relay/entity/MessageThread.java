package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks metadata for a message thread.
 *
 * <p>Created when the first reply is posted to a root message.
 * Maintains reply count, last reply timestamp, and participant list
 * for efficient thread summary rendering.</p>
 */
@Entity
@Table(name = "message_threads",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_thread_root_message",
                columnNames = {"root_message_id"}),
        indexes = {
                @Index(name = "idx_thread_channel_id", columnList = "channel_id"),
                @Index(name = "idx_thread_last_reply_at", columnList = "last_reply_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageThread extends BaseEntity {

    /** The parent message that started this thread. */
    @Column(name = "root_message_id", nullable = false)
    private UUID rootMessageId;

    /** Channel containing this thread. */
    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    /** Number of replies in this thread. */
    @Builder.Default
    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    /** Timestamp of the most recent reply. */
    @Column(name = "last_reply_at")
    private Instant lastReplyAt;

    /** User ID of the most recent replier. */
    @Column(name = "last_reply_by")
    private UUID lastReplyBy;

    /** Comma-separated UUIDs of users who have participated in this thread. */
    @Column(name = "participant_ids", columnDefinition = "TEXT")
    private String participantIds;
}
