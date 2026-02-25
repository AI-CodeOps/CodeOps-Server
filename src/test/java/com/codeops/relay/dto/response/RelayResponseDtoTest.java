package com.codeops.relay.dto.response;

import com.codeops.relay.entity.enums.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Construction and field access tests for all Relay module response DTOs.
 *
 * <p>Verifies that each response record can be instantiated with all fields,
 * handles null optional fields, and properly stores nested objects.</p>
 */
class RelayResponseDtoTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    // ── ChannelResponse ───────────────────────────────────────────────────

    @Nested
    class ChannelResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID projectId = UUID.randomUUID();
            var resp = new ChannelResponse(ID, "General", "general", "Main channel",
                    "Welcome!", ChannelType.PUBLIC, TEAM_ID, projectId, null,
                    false, USER_ID, 42, NOW, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("General");
            assertThat(resp.slug()).isEqualTo("general");
            assertThat(resp.description()).isEqualTo("Main channel");
            assertThat(resp.topic()).isEqualTo("Welcome!");
            assertThat(resp.channelType()).isEqualTo(ChannelType.PUBLIC);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.projectId()).isEqualTo(projectId);
            assertThat(resp.serviceId()).isNull();
            assertThat(resp.isArchived()).isFalse();
            assertThat(resp.createdBy()).isEqualTo(USER_ID);
            assertThat(resp.memberCount()).isEqualTo(42);
            assertThat(resp.createdAt()).isEqualTo(NOW);
            assertThat(resp.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void nullOptionalFields_constructsCorrectly() {
            var resp = new ChannelResponse(ID, "Dev", "dev", null, null,
                    ChannelType.PRIVATE, TEAM_ID, null, null, true, USER_ID, 0, NOW, NOW);

            assertThat(resp.description()).isNull();
            assertThat(resp.topic()).isNull();
            assertThat(resp.isArchived()).isTrue();
        }
    }

    // ── ChannelSummaryResponse ────────────────────────────────────────────

    @Nested
    class ChannelSummaryResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new ChannelSummaryResponse(ID, "General", "general",
                    ChannelType.PUBLIC, "Topic", false, 10, 5L, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.name()).isEqualTo("General");
            assertThat(resp.channelType()).isEqualTo(ChannelType.PUBLIC);
            assertThat(resp.isArchived()).isFalse();
            assertThat(resp.memberCount()).isEqualTo(10);
            assertThat(resp.unreadCount()).isEqualTo(5L);
            assertThat(resp.lastMessageAt()).isEqualTo(NOW);
        }
    }

    // ── ChannelMemberResponse ─────────────────────────────────────────────

    @Nested
    class ChannelMemberResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new ChannelMemberResponse(ID, CHANNEL_ID, USER_ID,
                    "John Doe", MemberRole.ADMIN, true, NOW, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.userDisplayName()).isEqualTo("John Doe");
            assertThat(resp.role()).isEqualTo(MemberRole.ADMIN);
            assertThat(resp.isMuted()).isTrue();
            assertThat(resp.lastReadAt()).isEqualTo(NOW);
            assertThat(resp.joinedAt()).isEqualTo(NOW);
        }

        @Test
        void mutedFalse_constructsCorrectly() {
            var resp = new ChannelMemberResponse(ID, CHANNEL_ID, USER_ID,
                    "Jane", MemberRole.MEMBER, false, null, NOW);

            assertThat(resp.isMuted()).isFalse();
            assertThat(resp.lastReadAt()).isNull();
        }
    }

    // ── MessageResponse ───────────────────────────────────────────────────

    @Nested
    class MessageResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID parentId = UUID.randomUUID();
            UUID platformEventId = UUID.randomUUID();
            List<UUID> mentionedIds = List.of(UUID.randomUUID());
            List<ReactionSummaryResponse> reactions = List.of(
                    new ReactionSummaryResponse("\uD83D\uDC4D", 3, true, List.of(USER_ID)));
            List<FileAttachmentResponse> attachments = Collections.emptyList();

            var resp = new MessageResponse(ID, CHANNEL_ID, USER_ID, "John", "Hello!",
                    MessageType.TEXT, parentId, true, NOW, false, true,
                    mentionedIds, platformEventId, reactions, attachments, 5, NOW, NOW, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.senderId()).isEqualTo(USER_ID);
            assertThat(resp.senderDisplayName()).isEqualTo("John");
            assertThat(resp.content()).isEqualTo("Hello!");
            assertThat(resp.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(resp.parentId()).isEqualTo(parentId);
            assertThat(resp.isEdited()).isTrue();
            assertThat(resp.editedAt()).isEqualTo(NOW);
            assertThat(resp.isDeleted()).isFalse();
            assertThat(resp.mentionsEveryone()).isTrue();
            assertThat(resp.mentionedUserIds()).isEqualTo(mentionedIds);
            assertThat(resp.platformEventId()).isEqualTo(platformEventId);
            assertThat(resp.reactions()).hasSize(1);
            assertThat(resp.attachments()).isEmpty();
            assertThat(resp.replyCount()).isEqualTo(5);
            assertThat(resp.lastReplyAt()).isEqualTo(NOW);
        }

        @Test
        void nullOptionalFields_constructsCorrectly() {
            var resp = new MessageResponse(ID, CHANNEL_ID, USER_ID, "John", "Hi",
                    MessageType.TEXT, null, false, null, false, false,
                    null, null, null, null, 0, null, NOW, NOW);

            assertThat(resp.parentId()).isNull();
            assertThat(resp.editedAt()).isNull();
            assertThat(resp.reactions()).isNull();
            assertThat(resp.attachments()).isNull();
            assertThat(resp.replyCount()).isZero();
        }
    }

    // ── MessageThreadResponse ─────────────────────────────────────────────

    @Nested
    class MessageThreadResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID rootId = UUID.randomUUID();
            UUID lastReplyBy = UUID.randomUUID();
            List<UUID> participantIds = List.of(USER_ID, lastReplyBy);

            var resp = new MessageThreadResponse(rootId, CHANNEL_ID, 3, NOW,
                    lastReplyBy, participantIds, Collections.emptyList());

            assertThat(resp.rootMessageId()).isEqualTo(rootId);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.replyCount()).isEqualTo(3);
            assertThat(resp.lastReplyAt()).isEqualTo(NOW);
            assertThat(resp.lastReplyBy()).isEqualTo(lastReplyBy);
            assertThat(resp.participantIds()).hasSize(2);
            assertThat(resp.replies()).isEmpty();
        }
    }

    // ── DirectConversationResponse ────────────────────────────────────────

    @Nested
    class DirectConversationResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            List<UUID> participants = List.of(USER_ID, UUID.randomUUID());

            var resp = new DirectConversationResponse(ID, TEAM_ID,
                    ConversationType.ONE_ON_ONE, null, participants, NOW, "Last msg", NOW, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.conversationType()).isEqualTo(ConversationType.ONE_ON_ONE);
            assertThat(resp.name()).isNull();
            assertThat(resp.participantIds()).hasSize(2);
            assertThat(resp.lastMessageAt()).isEqualTo(NOW);
            assertThat(resp.lastMessagePreview()).isEqualTo("Last msg");
        }
    }

    // ── DirectConversationSummaryResponse ─────────────────────────────────

    @Nested
    class DirectConversationSummaryResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new DirectConversationSummaryResponse(ID,
                    ConversationType.GROUP, "Team Chat",
                    List.of(USER_ID), List.of("John Doe"), "Hey!", NOW, 3L);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.conversationType()).isEqualTo(ConversationType.GROUP);
            assertThat(resp.name()).isEqualTo("Team Chat");
            assertThat(resp.participantIds()).hasSize(1);
            assertThat(resp.participantDisplayNames()).containsExactly("John Doe");
            assertThat(resp.lastMessagePreview()).isEqualTo("Hey!");
            assertThat(resp.unreadCount()).isEqualTo(3L);
        }
    }

    // ── DirectMessageResponse ─────────────────────────────────────────────

    @Nested
    class DirectMessageResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID conversationId = UUID.randomUUID();

            var resp = new DirectMessageResponse(ID, conversationId, USER_ID,
                    "John", "Hello DM!", MessageType.TEXT, true, NOW, false,
                    Collections.emptyList(), Collections.emptyList(), NOW, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.conversationId()).isEqualTo(conversationId);
            assertThat(resp.senderId()).isEqualTo(USER_ID);
            assertThat(resp.isEdited()).isTrue();
            assertThat(resp.isDeleted()).isFalse();
            assertThat(resp.reactions()).isEmpty();
            assertThat(resp.attachments()).isEmpty();
        }
    }

    // ── ReactionResponse ──────────────────────────────────────────────────

    @Nested
    class ReactionResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new ReactionResponse(ID, USER_ID, "John", "\uD83D\uDE00",
                    ReactionType.EMOJI, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.userDisplayName()).isEqualTo("John");
            assertThat(resp.emoji()).isEqualTo("\uD83D\uDE00");
            assertThat(resp.reactionType()).isEqualTo(ReactionType.EMOJI);
            assertThat(resp.createdAt()).isEqualTo(NOW);
        }
    }

    // ── ReactionSummaryResponse ───────────────────────────────────────────

    @Nested
    class ReactionSummaryResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new ReactionSummaryResponse("\uD83D\uDC4D", 5, true,
                    List.of(USER_ID, UUID.randomUUID()));

            assertThat(resp.emoji()).isEqualTo("\uD83D\uDC4D");
            assertThat(resp.count()).isEqualTo(5);
            assertThat(resp.currentUserReacted()).isTrue();
            assertThat(resp.userIds()).hasSize(2);
        }

        @Test
        void notReacted_constructsCorrectly() {
            var resp = new ReactionSummaryResponse("\uD83D\uDE00", 1, false,
                    List.of(UUID.randomUUID()));

            assertThat(resp.currentUserReacted()).isFalse();
        }
    }

    // ── FileAttachmentResponse ────────────────────────────────────────────

    @Nested
    class FileAttachmentResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new FileAttachmentResponse(ID, "report.pdf",
                    "application/pdf", 1024L, "/download/123",
                    null, FileUploadStatus.COMPLETE, USER_ID, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.fileName()).isEqualTo("report.pdf");
            assertThat(resp.contentType()).isEqualTo("application/pdf");
            assertThat(resp.fileSizeBytes()).isEqualTo(1024L);
            assertThat(resp.downloadUrl()).isEqualTo("/download/123");
            assertThat(resp.thumbnailUrl()).isNull();
            assertThat(resp.status()).isEqualTo(FileUploadStatus.COMPLETE);
            assertThat(resp.uploadedBy()).isEqualTo(USER_ID);
        }

        @Test
        void withThumbnail_constructsCorrectly() {
            var resp = new FileAttachmentResponse(ID, "photo.jpg",
                    "image/jpeg", 2048L, "/download/456",
                    "/thumb/456", FileUploadStatus.COMPLETE, USER_ID, NOW);

            assertThat(resp.thumbnailUrl()).isEqualTo("/thumb/456");
        }
    }

    // ── PinnedMessageResponse ─────────────────────────────────────────────

    @Nested
    class PinnedMessageResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID messageId = UUID.randomUUID();
            UUID pinnedBy = UUID.randomUUID();
            var message = new MessageResponse(messageId, CHANNEL_ID, USER_ID,
                    "John", "Pin me!", MessageType.TEXT, null, false, null,
                    false, false, null, null, null, null, 0, null, NOW, NOW);

            var resp = new PinnedMessageResponse(ID, messageId, CHANNEL_ID,
                    message, pinnedBy, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.messageId()).isEqualTo(messageId);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.message()).isNotNull();
            assertThat(resp.message().content()).isEqualTo("Pin me!");
            assertThat(resp.pinnedBy()).isEqualTo(pinnedBy);
        }

        @Test
        void nullMessage_constructsCorrectly() {
            var resp = new PinnedMessageResponse(ID, UUID.randomUUID(),
                    CHANNEL_ID, null, UUID.randomUUID(), NOW);

            assertThat(resp.message()).isNull();
        }
    }

    // ── ReadReceiptResponse ───────────────────────────────────────────────

    @Nested
    class ReadReceiptResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID lastReadId = UUID.randomUUID();
            var resp = new ReadReceiptResponse(CHANNEL_ID, USER_ID, lastReadId, NOW);

            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.lastReadMessageId()).isEqualTo(lastReadId);
            assertThat(resp.lastReadAt()).isEqualTo(NOW);
        }
    }

    // ── UserPresenceResponse ──────────────────────────────────────────────

    @Nested
    class UserPresenceResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new UserPresenceResponse(USER_ID, "John Doe", TEAM_ID,
                    PresenceStatus.ONLINE, "Working hard", NOW, NOW);

            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.userDisplayName()).isEqualTo("John Doe");
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.status()).isEqualTo(PresenceStatus.ONLINE);
            assertThat(resp.statusMessage()).isEqualTo("Working hard");
            assertThat(resp.lastSeenAt()).isEqualTo(NOW);
        }

        @Test
        void offlineWithNulls_constructsCorrectly() {
            var resp = new UserPresenceResponse(USER_ID, "Jane", TEAM_ID,
                    PresenceStatus.OFFLINE, null, null, NOW);

            assertThat(resp.status()).isEqualTo(PresenceStatus.OFFLINE);
            assertThat(resp.statusMessage()).isNull();
            assertThat(resp.lastSeenAt()).isNull();
        }
    }

    // ── PlatformEventResponse ─────────────────────────────────────────────

    @Nested
    class PlatformEventResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID sourceEntityId = UUID.randomUUID();
            UUID targetChannelId = UUID.randomUUID();

            var resp = new PlatformEventResponse(ID, PlatformEventType.AUDIT_COMPLETED,
                    TEAM_ID, "logger", sourceEntityId, "Audit Done",
                    "Full detail here", targetChannelId, "general",
                    true, NOW, NOW);

            assertThat(resp.id()).isEqualTo(ID);
            assertThat(resp.eventType()).isEqualTo(PlatformEventType.AUDIT_COMPLETED);
            assertThat(resp.teamId()).isEqualTo(TEAM_ID);
            assertThat(resp.sourceModule()).isEqualTo("logger");
            assertThat(resp.sourceEntityId()).isEqualTo(sourceEntityId);
            assertThat(resp.title()).isEqualTo("Audit Done");
            assertThat(resp.detail()).isEqualTo("Full detail here");
            assertThat(resp.targetChannelId()).isEqualTo(targetChannelId);
            assertThat(resp.targetChannelSlug()).isEqualTo("general");
            assertThat(resp.isDelivered()).isTrue();
            assertThat(resp.deliveredAt()).isEqualTo(NOW);
        }

        @Test
        void undelivered_constructsCorrectly() {
            var resp = new PlatformEventResponse(ID, PlatformEventType.BUILD_COMPLETED,
                    TEAM_ID, "core", null, "Build Passed", null,
                    null, null, false, null, NOW);

            assertThat(resp.isDelivered()).isFalse();
            assertThat(resp.deliveredAt()).isNull();
            assertThat(resp.targetChannelId()).isNull();
        }
    }

    // ── UnreadCountResponse ───────────────────────────────────────────────

    @Nested
    class UnreadCountResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new UnreadCountResponse(CHANNEL_ID, "General", "general",
                    15L, NOW);

            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.channelName()).isEqualTo("General");
            assertThat(resp.channelSlug()).isEqualTo("general");
            assertThat(resp.unreadCount()).isEqualTo(15L);
            assertThat(resp.lastReadAt()).isEqualTo(NOW);
        }

        @Test
        void zeroUnread_constructsCorrectly() {
            var resp = new UnreadCountResponse(CHANNEL_ID, "Dev", "dev", 0L, NOW);

            assertThat(resp.unreadCount()).isZero();
        }
    }

    // ── ChannelSearchResultResponse ───────────────────────────────────────

    @Nested
    class ChannelSearchResultResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            UUID messageId = UUID.randomUUID();

            var resp = new ChannelSearchResultResponse(messageId, CHANNEL_ID,
                    "General", USER_ID, "John", "matched text...", NOW);

            assertThat(resp.messageId()).isEqualTo(messageId);
            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.channelName()).isEqualTo("General");
            assertThat(resp.senderId()).isEqualTo(USER_ID);
            assertThat(resp.senderDisplayName()).isEqualTo("John");
            assertThat(resp.contentSnippet()).isEqualTo("matched text...");
        }
    }

    // ── TypingIndicatorResponse ───────────────────────────────────────────

    @Nested
    class TypingIndicatorResponseTests {

        @Test
        void allFields_constructsCorrectly() {
            var resp = new TypingIndicatorResponse(CHANNEL_ID, USER_ID, "John", NOW);

            assertThat(resp.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(resp.userId()).isEqualTo(USER_ID);
            assertThat(resp.userDisplayName()).isEqualTo("John");
            assertThat(resp.timestamp()).isEqualTo(NOW);
        }
    }
}
