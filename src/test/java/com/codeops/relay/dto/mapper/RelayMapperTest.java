package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.request.CreateChannelRequest;
import com.codeops.relay.dto.request.SendDirectMessageRequest;
import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.dto.response.*;
import com.codeops.relay.entity.*;
import com.codeops.relay.entity.enums.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for all Relay module MapStruct mappers.
 *
 * <p>Verifies entity-to-response mapping (including boolean field name translation),
 * request-to-entity mapping (including constant defaults and ignored fields),
 * and proper handling of ignored/service-populated fields.</p>
 */
class RelayMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    // ── ChannelMapper ─────────────────────────────────────────────────────

    @Nested
    class ChannelMapperTests {

        private final ChannelMapper mapper = Mappers.getMapper(ChannelMapper.class);

        @Test
        void toEntity_mapsFieldsAndSetsDefaults() {
            var request = new CreateChannelRequest("General", "Main channel",
                    ChannelType.PUBLIC, "Welcome");

            Channel entity = mapper.toEntity(request);

            assertThat(entity.getName()).isEqualTo("General");
            assertThat(entity.getDescription()).isEqualTo("Main channel");
            assertThat(entity.getChannelType()).isEqualTo(ChannelType.PUBLIC);
            assertThat(entity.getTopic()).isEqualTo("Welcome");
            assertThat(entity.isArchived()).isFalse();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getTeamId()).isNull();
            assertThat(entity.getCreatedBy()).isNull();
            assertThat(entity.getSlug()).isNull();
            assertThat(entity.getProjectId()).isNull();
            assertThat(entity.getServiceId()).isNull();
        }

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsArchived() {
            Channel channel = Channel.builder()
                    .name("General")
                    .slug("general")
                    .description("Desc")
                    .topic("Topic")
                    .channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID)
                    .createdBy(USER_ID)
                    .isArchived(true)
                    .build();
            channel.setId(ID);
            channel.setCreatedAt(NOW);
            channel.setUpdatedAt(NOW);

            ChannelResponse resp = mapper.toResponse(channel);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("General");
            assertThat(resp.slug()).isEqualTo("general");
            assertThat(resp.description()).isEqualTo("Desc");
            assertThat(resp.topic()).isEqualTo("Topic");
            assertThat(resp.channelType()).isEqualTo(ChannelType.PUBLIC);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.createdBy()).isEqualTo(USER_ID);
            assertThat(resp.isArchived()).isTrue();
            assertThat(resp.memberCount()).isZero();
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_archivedFalse_mapsFalse() {
            Channel channel = Channel.builder()
                    .name("Test")
                    .slug("test")
                    .channelType(ChannelType.PRIVATE)
                    .teamId(TEAM_ID)
                    .createdBy(USER_ID)
                    .build();

            ChannelResponse resp = mapper.toResponse(channel);

            assertThat(resp.isArchived()).isFalse();
        }

        @Test
        void toSummaryResponse_mapsFieldsAndIgnoresServiceFields() {
            Channel channel = Channel.builder()
                    .name("General")
                    .slug("general")
                    .channelType(ChannelType.PUBLIC)
                    .topic("Topic")
                    .teamId(TEAM_ID)
                    .createdBy(USER_ID)
                    .isArchived(false)
                    .build();
            channel.setId(ID);

            ChannelSummaryResponse resp = mapper.toSummaryResponse(channel);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("General");
            assertThat(resp.slug()).isEqualTo("general");
            assertThat(resp.channelType()).isEqualTo(ChannelType.PUBLIC);
            assertThat(resp.topic()).isEqualTo("Topic");
            assertThat(resp.isArchived()).isFalse();
            assertThat(resp.memberCount()).isZero();
            assertThat(resp.unreadCount()).isZero();
            assertThat(resp.lastMessageAt()).isNull();
        }
    }

    // ── ChannelMemberMapper ───────────────────────────────────────────────

    @Nested
    class ChannelMemberMapperTests {

        private final ChannelMemberMapper mapper = Mappers.getMapper(ChannelMemberMapper.class);

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsMuted() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID)
                    .userId(USER_ID)
                    .role(MemberRole.ADMIN)
                    .isMuted(true)
                    .lastReadAt(NOW)
                    .joinedAt(NOW)
                    .build();
            member.setId(ID);

            ChannelMemberResponse resp = mapper.toResponse(member);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.role()).isEqualTo(MemberRole.ADMIN);
            assertThat(resp.isMuted()).isTrue();
            assertThat(resp.lastReadAt()).isEqualTo(NOW);
            assertThat(resp.joinedAt()).isEqualTo(NOW);
            assertThat(resp.userDisplayName()).isNull();
        }

        @Test
        void toResponse_mutedFalse_mapsFalse() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID)
                    .userId(USER_ID)
                    .role(MemberRole.MEMBER)
                    .joinedAt(NOW)
                    .build();

            ChannelMemberResponse resp = mapper.toResponse(member);

            assertThat(resp.isMuted()).isFalse();
        }
    }

    // ── MessageMapper ─────────────────────────────────────────────────────

    @Nested
    class MessageMapperTests {

        private final MessageMapper mapper = Mappers.getMapper(MessageMapper.class);

        @Test
        void toEntity_mapsContentAndSetsConstants() {
            var request = new SendMessageRequest("Hello!", null, null, null);

            Message entity = mapper.toEntity(request);

            assertThat(entity.getContent()).isEqualTo("Hello!");
            assertThat(entity.getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(entity.isEdited()).isFalse();
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getChannelId()).isNull();
            assertThat(entity.getSenderId()).isNull();
            assertThat(entity.getEditedAt()).isNull();
            assertThat(entity.getPlatformEventId()).isNull();
        }

        @Test
        void toEntity_withParentId_mapsParentId() {
            UUID parentId = UUID.randomUUID();
            var request = new SendMessageRequest("Reply", parentId, null, null);

            Message entity = mapper.toEntity(request);

            assertThat(entity.getParentId()).isEqualTo(parentId);
            assertThat(entity.getContent()).isEqualTo("Reply");
        }

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsEditedAndIsDeleted() {
            Message message = Message.builder()
                    .channelId(CHANNEL_ID)
                    .senderId(USER_ID)
                    .content("Hello!")
                    .messageType(MessageType.TEXT)
                    .isEdited(true)
                    .editedAt(NOW)
                    .isDeleted(false)
                    .mentionsEveryone(true)
                    .build();
            message.setId(ID);
            message.setCreatedAt(NOW);
            message.setUpdatedAt(NOW);

            MessageResponse resp = mapper.toResponse(message);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.senderId()).isEqualTo(USER_ID);
            assertThat(resp.content()).isEqualTo("Hello!");
            assertThat(resp.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(resp.isEdited()).isTrue();
            assertThat(resp.editedAt()).isEqualTo(NOW);
            assertThat(resp.isDeleted()).isFalse();
            assertThat(resp.mentionsEveryone()).isTrue();
            assertThat(resp.senderDisplayName()).isNull();
            assertThat(resp.reactions()).isNull();
            assertThat(resp.attachments()).isNull();
            assertThat(resp.mentionedUserIds()).isNull();
            assertThat(resp.replyCount()).isZero();
            assertThat(resp.lastReplyAt()).isNull();
        }

        @Test
        void toResponse_deletedMessage_mapsDeletedTrue() {
            Message message = Message.builder()
                    .channelId(CHANNEL_ID)
                    .senderId(USER_ID)
                    .content("[deleted]")
                    .messageType(MessageType.TEXT)
                    .isDeleted(true)
                    .isEdited(false)
                    .build();

            MessageResponse resp = mapper.toResponse(message);

            assertThat(resp.isDeleted()).isTrue();
            assertThat(resp.isEdited()).isFalse();
        }
    }

    // ── MessageThreadMapper ───────────────────────────────────────────────

    @Nested
    class MessageThreadMapperTests {

        private final MessageThreadMapper mapper = Mappers.getMapper(MessageThreadMapper.class);

        @Test
        void toResponse_mapsFieldsAndIgnoresServiceFields() {
            UUID rootId = UUID.randomUUID();
            UUID lastReplyBy = UUID.randomUUID();

            MessageThread thread = MessageThread.builder()
                    .rootMessageId(rootId)
                    .channelId(CHANNEL_ID)
                    .replyCount(5)
                    .lastReplyAt(NOW)
                    .lastReplyBy(lastReplyBy)
                    .build();

            MessageThreadResponse resp = mapper.toResponse(thread);

            assertThat(resp.rootMessageId()).isEqualTo(rootId);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.replyCount()).isEqualTo(5);
            assertThat(resp.lastReplyAt()).isEqualTo(NOW);
            assertThat(resp.lastReplyBy()).isEqualTo(lastReplyBy);
            assertThat(resp.participantIds()).isNull();
            assertThat(resp.replies()).isNull();
        }
    }

    // ── DirectConversationMapper ──────────────────────────────────────────

    @Nested
    class DirectConversationMapperTests {

        private final DirectConversationMapper mapper = Mappers.getMapper(DirectConversationMapper.class);

        @Test
        void toResponse_mapsFieldsAndIgnoresParticipantIds() {
            DirectConversation conv = DirectConversation.builder()
                    .teamId(TEAM_ID)
                    .conversationType(ConversationType.ONE_ON_ONE)
                    .lastMessageAt(NOW)
                    .lastMessagePreview("Hey!")
                    .build();
            conv.setId(ID);
            conv.setCreatedAt(NOW);
            conv.setUpdatedAt(NOW);

            DirectConversationResponse resp = mapper.toResponse(conv);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.conversationType()).isEqualTo(ConversationType.ONE_ON_ONE);
            assertThat(resp.lastMessageAt()).isEqualTo(NOW);
            assertThat(resp.lastMessagePreview()).isEqualTo("Hey!");
            assertThat(resp.participantIds()).isNull();
        }

        @Test
        void toSummaryResponse_mapsFieldsAndIgnoresServiceFields() {
            DirectConversation conv = DirectConversation.builder()
                    .conversationType(ConversationType.GROUP)
                    .name("Group Chat")
                    .lastMessagePreview("Latest msg")
                    .lastMessageAt(NOW)
                    .teamId(TEAM_ID)
                    .build();
            conv.setId(ID);

            DirectConversationSummaryResponse resp = mapper.toSummaryResponse(conv);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.conversationType()).isEqualTo(ConversationType.GROUP);
            assertThat(resp.name()).isEqualTo("Group Chat");
            assertThat(resp.lastMessagePreview()).isEqualTo("Latest msg");
            assertThat(resp.participantIds()).isNull();
            assertThat(resp.participantDisplayNames()).isNull();
            assertThat(resp.unreadCount()).isZero();
        }
    }

    // ── DirectMessageMapper ───────────────────────────────────────────────

    @Nested
    class DirectMessageMapperTests {

        private final DirectMessageMapper mapper = Mappers.getMapper(DirectMessageMapper.class);

        @Test
        void toEntity_mapsContentAndSetsConstants() {
            var request = new SendDirectMessageRequest("Hello DM!");

            DirectMessage entity = mapper.toEntity(request);

            assertThat(entity.getContent()).isEqualTo("Hello DM!");
            assertThat(entity.getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(entity.isEdited()).isFalse();
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getConversationId()).isNull();
            assertThat(entity.getSenderId()).isNull();
        }

        @Test
        void toResponse_mapsFieldsIncludingBooleans() {
            DirectMessage dm = DirectMessage.builder()
                    .conversationId(UUID.randomUUID())
                    .senderId(USER_ID)
                    .content("Hi!")
                    .messageType(MessageType.TEXT)
                    .isEdited(true)
                    .editedAt(NOW)
                    .isDeleted(false)
                    .build();
            dm.setId(ID);
            dm.setCreatedAt(NOW);
            dm.setUpdatedAt(NOW);

            DirectMessageResponse resp = mapper.toResponse(dm);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.senderId()).isEqualTo(USER_ID);
            assertThat(resp.content()).isEqualTo("Hi!");
            assertThat(resp.isEdited()).isTrue();
            assertThat(resp.editedAt()).isEqualTo(NOW);
            assertThat(resp.isDeleted()).isFalse();
            assertThat(resp.senderDisplayName()).isNull();
            assertThat(resp.reactions()).isNull();
            assertThat(resp.attachments()).isNull();
        }
    }

    // ── ReactionMapper ────────────────────────────────────────────────────

    @Nested
    class ReactionMapperTests {

        private final ReactionMapper mapper = Mappers.getMapper(ReactionMapper.class);

        @Test
        void toResponse_mapsFieldsAndIgnoresDisplayName() {
            Reaction reaction = Reaction.builder()
                    .userId(USER_ID)
                    .emoji("\uD83D\uDC4D")
                    .reactionType(ReactionType.EMOJI)
                    .build();
            reaction.setId(ID);
            reaction.setCreatedAt(NOW);

            ReactionResponse resp = mapper.toResponse(reaction);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.emoji()).isEqualTo("\uD83D\uDC4D");
            assertThat(resp.reactionType()).isEqualTo(ReactionType.EMOJI);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.userDisplayName()).isNull();
        }
    }

    // ── FileAttachmentMapper ──────────────────────────────────────────────

    @Nested
    class FileAttachmentMapperTests {

        private final FileAttachmentMapper mapper = Mappers.getMapper(FileAttachmentMapper.class);

        @Test
        void toResponse_mapsFieldsAndIgnoresUrls() {
            FileAttachment attachment = FileAttachment.builder()
                    .fileName("report.pdf")
                    .contentType("application/pdf")
                    .fileSizeBytes(2048L)
                    .storagePath("/storage/files/report.pdf")
                    .status(FileUploadStatus.COMPLETE)
                    .uploadedBy(USER_ID)
                    .teamId(TEAM_ID)
                    .build();
            attachment.setId(ID);
            attachment.setCreatedAt(NOW);

            FileAttachmentResponse resp = mapper.toResponse(attachment);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.fileName()).isEqualTo("report.pdf");
            assertThat(resp.contentType()).isEqualTo("application/pdf");
            assertThat(resp.fileSizeBytes()).isEqualTo(2048L);
            assertThat(resp.status()).isEqualTo(FileUploadStatus.COMPLETE);
            assertThat(resp.uploadedBy()).isEqualTo(USER_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.downloadUrl()).isNull();
            assertThat(resp.thumbnailUrl()).isNull();
        }
    }

    // ── PinnedMessageMapper ───────────────────────────────────────────────

    @Nested
    class PinnedMessageMapperTests {

        private final PinnedMessageMapper mapper = Mappers.getMapper(PinnedMessageMapper.class);

        @Test
        void toResponse_extractsChannelIdFromEntity() {
            Channel channel = Channel.builder()
                    .name("General")
                    .slug("general")
                    .channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID)
                    .createdBy(USER_ID)
                    .build();
            channel.setId(CHANNEL_ID);

            UUID messageId = UUID.randomUUID();
            PinnedMessage pin = PinnedMessage.builder()
                    .messageId(messageId)
                    .pinnedBy(USER_ID)
                    .channel(channel)
                    .build();
            pin.setId(ID);
            pin.setCreatedAt(NOW);

            PinnedMessageResponse resp = mapper.toResponse(pin);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.messageId()).isEqualTo(messageId);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.pinnedBy()).isEqualTo(USER_ID);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.message()).isNull();
        }
    }

    // ── UserPresenceMapper ────────────────────────────────────────────────

    @Nested
    class UserPresenceMapperTests {

        private final UserPresenceMapper mapper = Mappers.getMapper(UserPresenceMapper.class);

        @Test
        void toResponse_mapsFieldsAndIgnoresDisplayName() {
            UserPresence presence = UserPresence.builder()
                    .userId(USER_ID)
                    .teamId(TEAM_ID)
                    .status(PresenceStatus.ONLINE)
                    .statusMessage("Available")
                    .lastSeenAt(NOW)
                    .build();
            presence.setUpdatedAt(NOW);

            UserPresenceResponse resp = mapper.toResponse(presence);

            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.status()).isEqualTo(PresenceStatus.ONLINE);
            assertThat(resp.statusMessage()).isEqualTo("Available");
            assertThat(resp.lastSeenAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
            assertThat(resp.userDisplayName()).isNull();
        }
    }

    // ── PlatformEventMapper ───────────────────────────────────────────────

    @Nested
    class PlatformEventMapperTests {

        private final PlatformEventMapper mapper = Mappers.getMapper(PlatformEventMapper.class);

        @Test
        void toResponse_mapsFieldsIncludingBooleanIsDelivered() {
            UUID sourceEntityId = UUID.randomUUID();
            UUID targetChannelId = UUID.randomUUID();

            PlatformEvent event = PlatformEvent.builder()
                    .eventType(PlatformEventType.AUDIT_COMPLETED)
                    .teamId(TEAM_ID)
                    .sourceModule("logger")
                    .sourceEntityId(sourceEntityId)
                    .title("Audit Done")
                    .detail("Full detail")
                    .targetChannelId(targetChannelId)
                    .targetChannelSlug("general")
                    .isDelivered(true)
                    .deliveredAt(NOW)
                    .build();
            event.setId(ID);
            event.setCreatedAt(NOW);

            PlatformEventResponse resp = mapper.toResponse(event);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.eventType()).isEqualTo(PlatformEventType.AUDIT_COMPLETED);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.sourceModule()).isEqualTo("logger");
            assertThat(resp.sourceEntityId()).isEqualTo(sourceEntityId);
            assertThat(resp.title()).isEqualTo("Audit Done");
            assertThat(resp.detail()).isEqualTo("Full detail");
            assertThat(resp.targetChannelId()).isEqualTo(targetChannelId);
            assertThat(resp.targetChannelSlug()).isEqualTo("general");
            assertThat(resp.isDelivered()).isTrue();
            assertThat(resp.deliveredAt()).isEqualTo(NOW);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }

        @Test
        void toResponse_undelivered_mapsFalse() {
            PlatformEvent event = PlatformEvent.builder()
                    .eventType(PlatformEventType.BUILD_COMPLETED)
                    .teamId(TEAM_ID)
                    .sourceModule("core")
                    .title("Build Passed")
                    .build();

            PlatformEventResponse resp = mapper.toResponse(event);

            assertThat(resp.isDelivered()).isFalse();
            assertThat(resp.deliveredAt()).isNull();
        }
    }
}
