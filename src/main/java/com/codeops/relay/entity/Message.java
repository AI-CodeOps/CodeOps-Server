package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a message sent in a channel.
 *
 * <p>Messages support Markdown content, threaded replies via {@code parentId},
 * soft deletion, @mentions, and attachments. Platform events from other
 * CodeOps modules are posted as messages with type PLATFORM_EVENT.</p>
 */
@Entity
@Table(name = "messages",
        indexes = {
                @Index(name = "idx_message_channel_id", columnList = "channel_id"),
                @Index(name = "idx_message_sender_id", columnList = "sender_id"),
                @Index(name = "idx_message_parent_id", columnList = "parent_id"),
                @Index(name = "idx_message_created_at", columnList = "created_at"),
                @Index(name = "idx_message_channel_created",
                        columnList = "channel_id, created_at DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    /** Channel this message belongs to. */
    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    /** User who sent this message. */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /** Markdown content of the message. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Type of message content. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;

    /** Parent message ID if this is a threaded reply. */
    @Column(name = "parent_id")
    private UUID parentId;

    /** Whether this message has been edited after initial posting. */
    @Builder.Default
    @Column(name = "is_edited", nullable = false)
    private boolean isEdited = false;

    /** When this message was last edited. */
    @Column(name = "edited_at")
    private Instant editedAt;

    /** Whether this message has been soft-deleted. */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /** Whether this message mentions everyone in the channel (@here or @all). */
    @Builder.Default
    @Column(name = "mentions_everyone", nullable = false)
    private boolean mentionsEveryone = false;

    /** Comma-separated UUIDs of @mentioned users. */
    @Column(name = "mentioned_user_ids", columnDefinition = "TEXT")
    private String mentionedUserIds;

    /** Reference to PlatformEvent if messageType is PLATFORM_EVENT. */
    @Column(name = "platform_event_id")
    private UUID platformEventId;

    /** Emoji reactions on this message. */
    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reaction> reactions = new ArrayList<>();

    /** File attachments on this message. */
    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileAttachment> attachments = new ArrayList<>();
}
