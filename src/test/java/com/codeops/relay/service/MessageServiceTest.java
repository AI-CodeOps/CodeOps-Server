package com.codeops.relay.service;

import com.codeops.dto.response.PageResponse;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.MessageMapper;
import com.codeops.relay.dto.request.MarkReadRequest;
import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.dto.request.UpdateMessageRequest;
import com.codeops.relay.dto.response.ChannelSearchResultResponse;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.dto.response.ReadReceiptResponse;
import com.codeops.relay.dto.response.UnreadCountResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.ChannelMember;
import com.codeops.relay.entity.FileAttachment;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.MessageThread;
import com.codeops.relay.entity.Reaction;
import com.codeops.relay.entity.ReadReceipt;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.FileUploadStatus;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.entity.enums.ReactionType;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.repository.FileAttachmentRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.ReactionRepository;
import com.codeops.relay.repository.ReadReceiptRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MessageService}.
 *
 * <p>Covers sending, getting, editing, deleting, searching, read receipts,
 * and helper methods for reaction summaries and message population.</p>
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private ChannelMemberRepository channelMemberRepository;
    @Mock private ReadReceiptRepository readReceiptRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageMapper messageMapper;
    @Mock private ThreadService threadService;
    @Mock private ReactionRepository reactionRepository;
    @Mock private FileAttachmentRepository fileAttachmentRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks private MessageService messageService;

    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private Channel publicChannel;
    private Channel archivedChannel;
    private ChannelMember membership;
    private User testUser;

    @BeforeEach
    void setUp() {
        publicChannel = Channel.builder()
                .name("General")
                .slug("general")
                .channelType(ChannelType.PUBLIC)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        publicChannel.setId(CHANNEL_ID);
        publicChannel.setCreatedAt(NOW);
        publicChannel.setUpdatedAt(NOW);

        archivedChannel = Channel.builder()
                .name("Archived")
                .slug("archived")
                .channelType(ChannelType.PUBLIC)
                .teamId(TEAM_ID)
                .isArchived(true)
                .createdBy(USER_ID)
                .build();
        archivedChannel.setId(UUID.randomUUID());
        archivedChannel.setCreatedAt(NOW);
        archivedChannel.setUpdatedAt(NOW);

        membership = ChannelMember.builder()
                .channelId(CHANNEL_ID)
                .userId(USER_ID)
                .role(MemberRole.MEMBER)
                .joinedAt(NOW)
                .build();
        membership.setId(UUID.randomUUID());

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
    }

    // ── Sending ───────────────────────────────────────────────────────────

    @Nested
    class SendMessageTests {

        @Test
        void sendMessage_success() {
            var request = new SendMessageRequest("Hello!", null, null, null);
            Message entity = buildMessage(MESSAGE_ID, "Hello!");

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageMapper.toEntity(request)).thenReturn(entity);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                m.setCreatedAt(NOW);
                m.setUpdatedAt(NOW);
                return m;
            });
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            MessageResponse result = messageService.sendMessage(
                    CHANNEL_ID, request, TEAM_ID, USER_ID);

            assertThat(result.content()).isEqualTo("Hello!");
            assertThat(result.senderDisplayName()).isEqualTo("Test User");
            assertThat(result.channelId()).isEqualTo(CHANNEL_ID);
            verify(readReceiptRepository).save(any(ReadReceipt.class));
        }

        @Test
        void sendMessage_notMember_throwsAuth() {
            var request = new SendMessageRequest("Hello!", null, null, null);
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendMessage(
                    CHANNEL_ID, request, TEAM_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("not a member");
        }

        @Test
        void sendMessage_archivedChannel_throwsValidation() {
            var request = new SendMessageRequest("Hello!", null, null, null);
            UUID archivedId = archivedChannel.getId();
            ChannelMember archivedMembership = ChannelMember.builder()
                    .channelId(archivedId)
                    .userId(USER_ID)
                    .role(MemberRole.MEMBER)
                    .joinedAt(NOW)
                    .build();

            when(channelRepository.findById(archivedId)).thenReturn(Optional.of(archivedChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(archivedId, USER_ID))
                    .thenReturn(Optional.of(archivedMembership));

            assertThatThrownBy(() -> messageService.sendMessage(
                    archivedId, request, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        void sendMessage_tooLong_throwsValidation() {
            var request = new SendMessageRequest("x".repeat(10001), null, null, null);
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));

            assertThatThrownBy(() -> messageService.sendMessage(
                    CHANNEL_ID, request, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum length");
        }

        @Test
        void sendMessage_withParentId_createsThreadReply() {
            UUID parentId = UUID.randomUUID();
            Message parentMessage = buildMessage(parentId, "Parent");
            parentMessage.setChannelId(CHANNEL_ID);
            var request = new SendMessageRequest("Reply!", parentId, null, null);
            Message entity = buildMessage(MESSAGE_ID, "Reply!");

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageMapper.toEntity(request)).thenReturn(entity);
            when(messageRepository.findById(parentId)).thenReturn(Optional.of(parentMessage));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                m.setCreatedAt(NOW);
                m.setUpdatedAt(NOW);
                return m;
            });
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            messageService.sendMessage(CHANNEL_ID, request, TEAM_ID, USER_ID);

            verify(threadService).onReply(parentId, CHANNEL_ID, USER_ID);
        }

        @Test
        void sendMessage_withMentions_serializesUserIds() {
            UUID mentioned1 = UUID.randomUUID();
            UUID mentioned2 = UUID.randomUUID();
            var request = new SendMessageRequest("Hey @folks!",
                    null, List.of(mentioned1, mentioned2), null);
            Message entity = buildMessage(MESSAGE_ID, "Hey @folks!");

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageMapper.toEntity(request)).thenReturn(entity);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                m.setCreatedAt(NOW);
                m.setUpdatedAt(NOW);
                return m;
            });
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            messageService.sendMessage(CHANNEL_ID, request, TEAM_ID, USER_ID);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().getMentionedUserIds())
                    .contains(mentioned1.toString())
                    .contains(mentioned2.toString());
        }

        @Test
        void sendMessage_mentionsEveryone_setsFlag() {
            var request = new SendMessageRequest("@all attention!", null, null, true);
            Message entity = buildMessage(MESSAGE_ID, "@all attention!");

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageMapper.toEntity(request)).thenReturn(entity);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                m.setCreatedAt(NOW);
                m.setUpdatedAt(NOW);
                return m;
            });
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            messageService.sendMessage(CHANNEL_ID, request, TEAM_ID, USER_ID);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().isMentionsEveryone()).isTrue();
        }

        @Test
        void sendMessage_updatesReadReceipt() {
            var request = new SendMessageRequest("Hello!", null, null, null);
            Message entity = buildMessage(MESSAGE_ID, "Hello!");

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageMapper.toEntity(request)).thenReturn(entity);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                m.setCreatedAt(NOW);
                m.setUpdatedAt(NOW);
                return m;
            });
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            messageService.sendMessage(CHANNEL_ID, request, TEAM_ID, USER_ID);

            ArgumentCaptor<ReadReceipt> captor = ArgumentCaptor.forClass(ReadReceipt.class);
            verify(readReceiptRepository).save(captor.capture());
            assertThat(captor.getValue().getLastReadMessageId()).isEqualTo(MESSAGE_ID);
        }
    }

    // ── Getting ───────────────────────────────────────────────────────────

    @Nested
    class GetMessageTests {

        @Test
        void getMessage_success_populatesAll() {
            Message message = buildMessage(MESSAGE_ID, "Hello!");
            message.setMentionedUserIds(UUID.randomUUID().toString());

            Reaction reaction = Reaction.builder()
                    .userId(USER_ID)
                    .emoji("\uD83D\uDE00")
                    .reactionType(ReactionType.EMOJI)
                    .messageId(MESSAGE_ID)
                    .build();
            reaction.setId(UUID.randomUUID());

            FileAttachment attachment = FileAttachment.builder()
                    .fileName("test.png")
                    .contentType("image/png")
                    .fileSizeBytes(1024L)
                    .storagePath("/path")
                    .uploadedBy(USER_ID)
                    .teamId(TEAM_ID)
                    .messageId(MESSAGE_ID)
                    .status(FileUploadStatus.COMPLETE)
                    .build();
            attachment.setId(UUID.randomUUID());
            attachment.setCreatedAt(NOW);

            MessageThread thread = MessageThread.builder()
                    .rootMessageId(MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(3)
                    .lastReplyAt(NOW)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(reaction));
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(attachment));
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.of(thread));

            MessageResponse result = messageService.getMessage(MESSAGE_ID);

            assertThat(result.senderDisplayName()).isEqualTo("Test User");
            assertThat(result.reactions()).hasSize(1);
            assertThat(result.reactions().get(0).emoji()).isEqualTo("\uD83D\uDE00");
            assertThat(result.attachments()).hasSize(1);
            assertThat(result.replyCount()).isEqualTo(3);
            assertThat(result.mentionedUserIds()).hasSize(1);
        }

        @Test
        void getMessage_notFound_throws() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getMessage(MESSAGE_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void getChannelMessages_paginatedNewestFirst() {
            Message m1 = buildMessage(UUID.randomUUID(), "Msg 1");
            Message m2 = buildMessage(UUID.randomUUID(), "Msg 2");
            Page<Message> page = new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 10), 2);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageRepository.findByChannelIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(CHANNEL_ID), any(Pageable.class))).thenReturn(page);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
            when(threadService.getThreadInfo(any(UUID.class))).thenReturn(Optional.empty());

            PageResponse<MessageResponse> result = messageService.getChannelMessages(
                    CHANNEL_ID, TEAM_ID, USER_ID, 0, 10);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        void getChannelMessages_notMember_throwsAuth() {
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getChannelMessages(
                    CHANNEL_ID, TEAM_ID, USER_ID, 0, 10))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void getThreadReplies_success() {
            Message parent = buildMessage(MESSAGE_ID, "Parent");
            parent.setChannelId(CHANNEL_ID);
            Message reply = buildMessage(UUID.randomUUID(), "Reply");

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(parent));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(MESSAGE_ID))
                    .thenReturn(List.of(reply));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
            when(threadService.getThreadInfo(any(UUID.class))).thenReturn(Optional.empty());

            List<MessageResponse> result = messageService.getThreadReplies(MESSAGE_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).content()).isEqualTo("Reply");
        }

        @Test
        void getThreadReplies_notMember_throwsAuth() {
            Message parent = buildMessage(MESSAGE_ID, "Parent");
            parent.setChannelId(CHANNEL_ID);
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(parent));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getThreadReplies(MESSAGE_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ── Editing ───────────────────────────────────────────────────────────

    @Nested
    class EditMessageTests {

        @Test
        void editMessage_bySender_success() {
            Message message = buildMessage(MESSAGE_ID, "Original");
            message.setSenderId(USER_ID);
            var request = new UpdateMessageRequest("Updated");

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            MessageResponse result = messageService.editMessage(MESSAGE_ID, request, USER_ID);

            assertThat(result.content()).isEqualTo("Updated");
            assertThat(result.isEdited()).isTrue();
        }

        @Test
        void editMessage_byOtherUser_throwsAuth() {
            Message message = buildMessage(MESSAGE_ID, "Original");
            message.setSenderId(OTHER_USER_ID);
            var request = new UpdateMessageRequest("Updated");

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            assertThatThrownBy(() -> messageService.editMessage(MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("sender");
        }

        @Test
        void editMessage_deletedMessage_throwsValidation() {
            Message message = buildMessage(MESSAGE_ID, "Deleted msg");
            message.setSenderId(USER_ID);
            message.setDeleted(true);
            var request = new UpdateMessageRequest("Updated");

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            assertThatThrownBy(() -> messageService.editMessage(MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("deleted");
        }

        @Test
        void editMessage_setsEditedFlag() {
            Message message = buildMessage(MESSAGE_ID, "Original");
            message.setSenderId(USER_ID);
            var request = new UpdateMessageRequest("Edited");

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            MessageResponse result = messageService.editMessage(MESSAGE_ID, request, USER_ID);

            assertThat(result.isEdited()).isTrue();
            assertThat(result.editedAt()).isNotNull();
        }
    }

    // ── Deleting ──────────────────────────────────────────────────────────

    @Nested
    class DeleteMessageTests {

        @Test
        void deleteMessage_bySender_success() {
            Message message = buildMessage(MESSAGE_ID, "To delete");
            message.setSenderId(USER_ID);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

            messageService.deleteMessage(MESSAGE_ID, USER_ID, TEAM_ID);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
            assertThat(captor.getValue().getContent()).isEqualTo("This message was deleted");
        }

        @Test
        void deleteMessage_byChannelAdmin_success() {
            Message message = buildMessage(MESSAGE_ID, "Someone else's msg");
            message.setSenderId(OTHER_USER_ID);

            ChannelMember adminMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID)
                    .userId(USER_ID)
                    .role(MemberRole.ADMIN)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(adminMember));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

            messageService.deleteMessage(MESSAGE_ID, USER_ID, TEAM_ID);

            verify(messageRepository).save(any(Message.class));
        }

        @Test
        void deleteMessage_byTeamAdmin_success() {
            Message message = buildMessage(MESSAGE_ID, "Someone else's msg");
            message.setSenderId(OTHER_USER_ID);

            TeamMember teamAdmin = new TeamMember();
            teamAdmin.setRole(TeamRole.ADMIN);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership)); // Regular member
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(teamAdmin));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

            messageService.deleteMessage(MESSAGE_ID, USER_ID, TEAM_ID);

            verify(messageRepository).save(any(Message.class));
        }

        @Test
        void deleteMessage_byRegularMember_throwsAuth() {
            Message message = buildMessage(MESSAGE_ID, "Someone else's msg");
            message.setSenderId(OTHER_USER_ID);

            TeamMember teamMember = new TeamMember();
            teamMember.setRole(TeamRole.MEMBER);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(teamMember));

            assertThatThrownBy(() -> messageService.deleteMessage(MESSAGE_ID, USER_ID, TEAM_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void deleteMessage_softDeletes() {
            Message message = buildMessage(MESSAGE_ID, "Original content");
            message.setSenderId(USER_ID);

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

            messageService.deleteMessage(MESSAGE_ID, USER_ID, TEAM_ID);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
            assertThat(captor.getValue().getContent()).isEqualTo("This message was deleted");
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    @Nested
    class SearchTests {

        @Test
        void searchMessages_inChannel_success() {
            Message match = buildMessage(UUID.randomUUID(), "foo bar baz");
            Page<Message> page = new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageRepository.searchInChannel(eq(CHANNEL_ID), eq("bar"), any(Pageable.class)))
                    .thenReturn(page);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(any(UUID.class))).thenReturn(List.of());
            when(threadService.getThreadInfo(any(UUID.class))).thenReturn(Optional.empty());

            PageResponse<MessageResponse> result = messageService.searchMessages(
                    CHANNEL_ID, "bar", TEAM_ID, USER_ID, 0, 10);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        void searchMessages_blankQuery_throwsValidation() {
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));

            assertThatThrownBy(() -> messageService.searchMessages(
                    CHANNEL_ID, "  ", TEAM_ID, USER_ID, 0, 10))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        void searchMessagesAcrossChannels_onlyUserChannels() {
            Message match = buildMessage(UUID.randomUUID(), "found it");
            match.setChannelId(CHANNEL_ID);
            Page<Message> page = new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1);

            when(channelMemberRepository.findChannelIdsByUserId(USER_ID))
                    .thenReturn(List.of(CHANNEL_ID));
            when(messageRepository.searchAcrossChannels(anyList(), eq("found"), any(Pageable.class)))
                    .thenReturn(page);
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            PageResponse<ChannelSearchResultResponse> result =
                    messageService.searchMessagesAcrossChannels(
                            "found", TEAM_ID, USER_ID, 0, 10);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).channelName()).isEqualTo("General");
        }

        @Test
        void searchMessagesAcrossChannels_buildsSnippet() {
            String longContent = "A".repeat(60) + "MATCH" + "B".repeat(60);
            Message match = buildMessage(UUID.randomUUID(), longContent);
            match.setChannelId(CHANNEL_ID);
            Page<Message> page = new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1);

            when(channelMemberRepository.findChannelIdsByUserId(USER_ID))
                    .thenReturn(List.of(CHANNEL_ID));
            when(messageRepository.searchAcrossChannels(anyList(), eq("MATCH"), any(Pageable.class)))
                    .thenReturn(page);
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            PageResponse<ChannelSearchResultResponse> result =
                    messageService.searchMessagesAcrossChannels(
                            "MATCH", TEAM_ID, USER_ID, 0, 10);

            assertThat(result.content().get(0).contentSnippet())
                    .startsWith("...")
                    .contains("MATCH")
                    .endsWith("...");
        }
    }

    // ── Read Receipts ─────────────────────────────────────────────────────

    @Nested
    class ReadReceiptTests {

        @Test
        void markRead_createsNew() {
            UUID msgId = UUID.randomUUID();
            var request = new MarkReadRequest(msgId);

            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ReadReceiptResponse result = messageService.markRead(CHANNEL_ID, request, USER_ID);

            assertThat(result.lastReadMessageId()).isEqualTo(msgId);
            assertThat(result.channelId()).isEqualTo(CHANNEL_ID);
            assertThat(result.userId()).isEqualTo(USER_ID);
            verify(readReceiptRepository).save(any(ReadReceipt.class));
        }

        @Test
        void markRead_updatesExisting() {
            UUID msgId = UUID.randomUUID();
            var request = new MarkReadRequest(msgId);
            ReadReceipt existing = ReadReceipt.builder()
                    .channelId(CHANNEL_ID)
                    .userId(USER_ID)
                    .lastReadMessageId(UUID.randomUUID())
                    .lastReadAt(NOW.minusSeconds(600))
                    .build();

            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ReadReceiptResponse result = messageService.markRead(CHANNEL_ID, request, USER_ID);

            assertThat(result.lastReadMessageId()).isEqualTo(msgId);
            ArgumentCaptor<ReadReceipt> captor = ArgumentCaptor.forClass(ReadReceipt.class);
            verify(readReceiptRepository).save(captor.capture());
            assertThat(captor.getValue().getLastReadMessageId()).isEqualTo(msgId);
        }

        @Test
        void getUnreadCounts_multipleChannels() {
            UUID channel2Id = UUID.randomUUID();
            Channel channel2 = Channel.builder()
                    .name("Dev")
                    .slug("dev")
                    .channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID)
                    .createdBy(USER_ID)
                    .build();
            channel2.setId(channel2Id);

            ChannelMember membership2 = ChannelMember.builder()
                    .channelId(channel2Id)
                    .userId(USER_ID)
                    .role(MemberRole.MEMBER)
                    .lastReadAt(NOW.minusSeconds(300))
                    .build();

            when(channelMemberRepository.findChannelIdsByUserId(USER_ID))
                    .thenReturn(List.of(CHANNEL_ID, channel2Id));
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelRepository.findById(channel2Id)).thenReturn(Optional.of(channel2));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(channelMemberRepository.findByChannelIdAndUserId(channel2Id, USER_ID))
                    .thenReturn(Optional.of(membership2));
            when(messageRepository.countUnreadMessages(eq(CHANNEL_ID), any(Instant.class)))
                    .thenReturn(5L);
            when(messageRepository.countUnreadMessages(eq(channel2Id), any(Instant.class)))
                    .thenReturn(3L);

            List<UnreadCountResponse> result = messageService.getUnreadCounts(TEAM_ID, USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).unreadCount()).isEqualTo(5);
            assertThat(result.get(1).unreadCount()).isEqualTo(3);
        }

        @Test
        void getUnreadCounts_noUnread_excluded() {
            when(channelMemberRepository.findChannelIdsByUserId(USER_ID))
                    .thenReturn(List.of(CHANNEL_ID));
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageRepository.countUnreadMessages(eq(CHANNEL_ID), any(Instant.class)))
                    .thenReturn(0L);

            List<UnreadCountResponse> result = messageService.getUnreadCounts(TEAM_ID, USER_ID);

            assertThat(result).isEmpty();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    @Nested
    class HelperTests {

        @Test
        void buildReactionSummaries_groupsByEmoji() {
            Message message = buildMessage(MESSAGE_ID, "Hello!");
            Reaction r1 = buildReaction(UUID.randomUUID(), USER_ID, "\uD83D\uDE00");
            Reaction r2 = buildReaction(UUID.randomUUID(), OTHER_USER_ID, "\uD83D\uDE00");
            Reaction r3 = buildReaction(UUID.randomUUID(), USER_ID, "\uD83D\uDC4D");

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID))
                    .thenReturn(List.of(r1, r2, r3));
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            MessageResponse result = messageService.getMessage(MESSAGE_ID);

            assertThat(result.reactions()).hasSize(2);
            // Sorted by count DESC: grinning face (2) before thumbs up (1)
            assertThat(result.reactions().get(0).count()).isEqualTo(2);
            assertThat(result.reactions().get(0).emoji()).isEqualTo("\uD83D\uDE00");
            assertThat(result.reactions().get(1).count()).isEqualTo(1);
        }

        @Test
        void buildReactionSummaries_currentUserReacted() {
            Message message = buildMessage(MESSAGE_ID, "Hello!");
            Reaction r1 = buildReaction(UUID.randomUUID(), USER_ID, "\uD83D\uDE00");

            // getMessage passes null for currentUserId, so let's use the sendMessage path
            // which passes senderId. We'll just test the getMessage path here.
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(r1));
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.empty());

            // getMessage passes null as currentUserId
            MessageResponse result = messageService.getMessage(MESSAGE_ID);

            // With null currentUserId, currentUserReacted should be false
            assertThat(result.reactions().get(0).currentUserReacted()).isFalse();
            assertThat(result.reactions().get(0).userIds()).contains(USER_ID);
        }

        @Test
        void populateMessageResponse_withThread() {
            Message message = buildMessage(MESSAGE_ID, "Root");
            MessageThread thread = MessageThread.builder()
                    .rootMessageId(MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(7)
                    .lastReplyAt(NOW)
                    .build();

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());
            when(threadService.getThreadInfo(MESSAGE_ID)).thenReturn(Optional.of(thread));

            MessageResponse result = messageService.getMessage(MESSAGE_ID);

            assertThat(result.replyCount()).isEqualTo(7);
            assertThat(result.lastReplyAt()).isEqualTo(NOW);
        }
    }

    // ── Test Data Builders ────────────────────────────────────────────────

    private Message buildMessage(UUID id, String content) {
        Message msg = Message.builder()
                .channelId(CHANNEL_ID)
                .senderId(USER_ID)
                .content(content)
                .messageType(MessageType.TEXT)
                .build();
        msg.setId(id);
        msg.setCreatedAt(NOW);
        msg.setUpdatedAt(NOW);
        return msg;
    }

    private Reaction buildReaction(UUID id, UUID userId, String emoji) {
        Reaction reaction = Reaction.builder()
                .userId(userId)
                .emoji(emoji)
                .reactionType(ReactionType.EMOJI)
                .messageId(MESSAGE_ID)
                .build();
        reaction.setId(id);
        return reaction;
    }
}
