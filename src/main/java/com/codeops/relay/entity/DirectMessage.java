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
 * Represents a message sent in a direct conversation.
 *
 * <p>Supports text content, editing, soft deletion, and file attachments.
 * Reactions can be added via the {@link Reaction} entity.</p>
 */
@Entity
@Table(name = "direct_messages",
        indexes = {
                @Index(name = "idx_dm_message_conversation_id", columnList = "conversation_id"),
                @Index(name = "idx_dm_message_created_at", columnList = "created_at"),
                @Index(name = "idx_dm_message_conv_created",
                        columnList = "conversation_id, created_at DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectMessage extends BaseEntity {

    /** Conversation this message belongs to. */
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    /** User who sent this message. */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /** Text content of the message. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Type of message content. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;

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

    /** The parent conversation entity. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)
    private DirectConversation conversation;

    /** Emoji reactions on this message. */
    @Builder.Default
    @OneToMany(mappedBy = "directMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reaction> reactions = new ArrayList<>();

    /** File attachments on this message. */
    @Builder.Default
    @OneToMany(mappedBy = "directMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileAttachment> attachments = new ArrayList<>();
}
