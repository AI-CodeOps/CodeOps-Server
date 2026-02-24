package com.codeops.relay.entity;

import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.entity.enums.ReactionType;
import com.codeops.relay.entity.enums.FileUploadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Message} entity.
 *
 * <p>Verifies message creation, thread replies via parentId,
 * soft delete behavior, default field values, and cascade
 * behavior for reactions and file attachments.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class MessageTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void createMessage_allFields_persistsCorrectly() {
        UUID channelId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        Message message = Message.builder()
                .channelId(channelId)
                .senderId(senderId)
                .content("Hello, team!")
                .messageType(MessageType.TEXT)
                .build();

        Message saved = em.persistAndFlush(message);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChannelId()).isEqualTo(channelId);
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getContent()).isEqualTo("Hello, team!");
        assertThat(saved.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void messageType_defaultsToText() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Default type")
                .build();

        Message saved = em.persistAndFlush(message);

        assertThat(saved.getMessageType()).isEqualTo(MessageType.TEXT);
    }

    @Test
    void threadReply_setsParentId() {
        UUID parentId = UUID.randomUUID();

        Message reply = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Thread reply")
                .parentId(parentId)
                .build();

        Message saved = em.persistAndFlush(reply);

        assertThat(saved.getParentId()).isEqualTo(parentId);
    }

    @Test
    void softDelete_setsIsDeletedFlag() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Will be deleted")
                .build();

        Message saved = em.persistAndFlush(message);
        assertThat(saved.isDeleted()).isFalse();

        saved.setDeleted(true);
        em.persistAndFlush(saved);
        em.clear();

        Message found = em.find(Message.class, saved.getId());
        assertThat(found.isDeleted()).isTrue();
    }

    @Test
    void defaultBooleanFields_areFalse() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Defaults test")
                .build();

        Message saved = em.persistAndFlush(message);

        assertThat(saved.isEdited()).isFalse();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.isMentionsEveryone()).isFalse();
    }

    @Test
    void editedMessage_setsEditedFields() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Original")
                .build();

        Message saved = em.persistAndFlush(message);

        saved.setContent("Edited content");
        saved.setEdited(true);
        saved.setEditedAt(Instant.now());
        em.persistAndFlush(saved);
        em.clear();

        Message found = em.find(Message.class, saved.getId());
        assertThat(found.isEdited()).isTrue();
        assertThat(found.getEditedAt()).isNotNull();
        assertThat(found.getContent()).isEqualTo("Edited content");
    }

    @Test
    void platformEventMessage_setsPlatformEventId() {
        UUID eventId = UUID.randomUUID();

        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("Deployment completed")
                .messageType(MessageType.PLATFORM_EVENT)
                .platformEventId(eventId)
                .build();

        Message saved = em.persistAndFlush(message);

        assertThat(saved.getMessageType()).isEqualTo(MessageType.PLATFORM_EVENT);
        assertThat(saved.getPlatformEventId()).isEqualTo(eventId);
    }

    @Test
    void mentions_setsMentionFields() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("@here check this out")
                .mentionsEveryone(true)
                .mentionedUserIds(UUID.randomUUID().toString())
                .build();

        Message saved = em.persistAndFlush(message);

        assertThat(saved.isMentionsEveryone()).isTrue();
        assertThat(saved.getMentionedUserIds()).isNotNull();
    }

    @Test
    void reactionsCascade_persistsWithMessage() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("React to this")
                .build();

        Message saved = em.persistAndFlush(message);

        Reaction reaction = Reaction.builder()
                .userId(UUID.randomUUID())
                .emoji("\uD83D\uDC4D")
                .reactionType(ReactionType.EMOJI)
                .messageId(saved.getId())
                .build();
        saved.getReactions().add(reaction);

        em.persistAndFlush(saved);
        em.clear();

        Message found = em.find(Message.class, saved.getId());
        assertThat(found.getReactions()).hasSize(1);
        assertThat(found.getReactions().get(0).getEmoji()).isEqualTo("\uD83D\uDC4D");
    }

    @Test
    void attachmentsCascade_persistsWithMessage() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("File attached")
                .messageType(MessageType.FILE)
                .build();

        Message saved = em.persistAndFlush(message);

        FileAttachment attachment = FileAttachment.builder()
                .fileName("report.pdf")
                .contentType("application/pdf")
                .fileSizeBytes(1024L)
                .storagePath("/storage/report.pdf")
                .status(FileUploadStatus.COMPLETE)
                .uploadedBy(UUID.randomUUID())
                .teamId(UUID.randomUUID())
                .messageId(saved.getId())
                .build();
        saved.getAttachments().add(attachment);

        em.persistAndFlush(saved);
        em.clear();

        Message found = em.find(Message.class, saved.getId());
        assertThat(found.getAttachments()).hasSize(1);
        assertThat(found.getAttachments().get(0).getFileName()).isEqualTo("report.pdf");
    }

    @Test
    void messageType_persistsAllEnumValues() {
        for (MessageType type : MessageType.values()) {
            Message message = Message.builder()
                    .channelId(UUID.randomUUID())
                    .senderId(UUID.randomUUID())
                    .content("Type: " + type.name())
                    .messageType(type)
                    .build();

            Message saved = em.persistAndFlush(message);
            em.clear();

            Message found = em.find(Message.class, saved.getId());
            assertThat(found.getMessageType()).isEqualTo(type);
        }
    }
}
