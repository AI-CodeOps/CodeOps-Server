package com.codeops.relay.service;

import com.codeops.dto.response.PageResponse;
import com.codeops.entity.User;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.DirectConversationMapper;
import com.codeops.relay.dto.mapper.DirectMessageMapper;
import com.codeops.relay.dto.request.CreateDirectConversationRequest;
import com.codeops.relay.dto.request.SendDirectMessageRequest;
import com.codeops.relay.dto.request.UpdateDirectMessageRequest;
import com.codeops.relay.dto.response.DirectConversationResponse;
import com.codeops.relay.dto.response.DirectConversationSummaryResponse;
import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.entity.DirectConversation;
import com.codeops.relay.entity.DirectMessage;
import com.codeops.relay.entity.ReadReceipt;
import com.codeops.relay.entity.enums.ConversationType;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.repository.DirectConversationRepository;
import com.codeops.relay.repository.DirectMessageRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DirectMessageService}.
 *
 * <p>Covers conversation management, message operations, read tracking,
 * and private helper methods.</p>
 */
@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock private DirectConversationRepository directConversationRepository;
    @Mock private DirectMessageRepository directMessageRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private DirectConversationMapper directConversationMapper;
    @Mock private DirectMessageMapper directMessageMapper;
    @Mock private ReadReceiptRepository readReceiptRepository;

    @InjectMocks private DirectMessageService directMessageService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private String sortedParticipantIds;
    private DirectConversation conversation;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        sortedParticipantIds = DirectMessageService.buildParticipantIds(USER_ID, OTHER_USER_ID);

        conversation = DirectConversation.builder()
                .teamId(TEAM_ID)
                .conversationType(ConversationType.ONE_ON_ONE)
                .participantIds(sortedParticipantIds)
                .build();
        conversation.setId(CONVERSATION_ID);
        conversation.setCreatedAt(NOW);
        conversation.setUpdatedAt(NOW);

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");

        otherUser = new User();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setDisplayName("Other User");
        otherUser.setEmail("other@example.com");
    }

    // ── Conversation Management ───────────────────────────────────────────

    @Nested
    class ConversationManagementTests {

        @Test
        void getOrCreateConversation_createsNew() {
            var request = new CreateDirectConversationRequest(List.of(OTHER_USER_ID), null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(true);
            when(directConversationRepository.findByTeamIdAndParticipantIds(
                    TEAM_ID, sortedParticipantIds)).thenReturn(Optional.empty());
            when(directConversationRepository.save(any(DirectConversation.class))).thenAnswer(inv -> {
                DirectConversation c = inv.getArgument(0);
                c.setId(CONVERSATION_ID);
                c.setCreatedAt(NOW);
                c.setUpdatedAt(NOW);
                return c;
            });

            DirectConversationResponse result = directMessageService.getOrCreateConversation(
                    request, TEAM_ID, USER_ID);

            assertThat(result.id()).isEqualTo(CONVERSATION_ID);
            assertThat(result.participantIds()).hasSize(2);
            assertThat(result.conversationType()).isEqualTo(ConversationType.ONE_ON_ONE);
            verify(directConversationRepository).save(any(DirectConversation.class));
        }

        @Test
        void getOrCreateConversation_returnsExisting() {
            var request = new CreateDirectConversationRequest(List.of(OTHER_USER_ID), null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(true);
            when(directConversationRepository.findByTeamIdAndParticipantIds(
                    TEAM_ID, sortedParticipantIds)).thenReturn(Optional.of(conversation));

            DirectConversationResponse result = directMessageService.getOrCreateConversation(
                    request, TEAM_ID, USER_ID);

            assertThat(result.id()).isEqualTo(CONVERSATION_ID);
            verify(directConversationRepository, never()).save(any());
        }

        @Test
        void getOrCreateConversation_selfDm_throwsValidation() {
            var request = new CreateDirectConversationRequest(List.of(USER_ID), null);

            assertThatThrownBy(() -> directMessageService.getOrCreateConversation(
                    request, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        void getOrCreateConversation_notTeamMember_throwsNotFound() {
            var request = new CreateDirectConversationRequest(List.of(OTHER_USER_ID), null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> directMessageService.getOrCreateConversation(
                    request, TEAM_ID, USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Target user");
        }

        @Test
        void getOrCreateConversation_sortedParticipantIds() {
            // Verify that regardless of who initiates, the participantIds string is the same
            String fromUser1 = DirectMessageService.buildParticipantIds(USER_ID, OTHER_USER_ID);
            String fromUser2 = DirectMessageService.buildParticipantIds(OTHER_USER_ID, USER_ID);
            assertThat(fromUser1).isEqualTo(fromUser2);
        }

        @Test
        void getConversation_success() {
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            DirectConversationResponse result = directMessageService.getConversation(
                    CONVERSATION_ID, USER_ID);

            assertThat(result.id()).isEqualTo(CONVERSATION_ID);
            assertThat(result.teamId()).isEqualTo(TEAM_ID);
        }

        @Test
        void getConversation_notParticipant_throwsAuth() {
            UUID outsider = UUID.randomUUID();
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> directMessageService.getConversation(
                    CONVERSATION_ID, outsider))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("not a participant");
        }

        @Test
        void getConversations_filtersSubstringMatches() {
            // Create a conversation where userId appears as substring but isn't a participant
            DirectConversation fakeMatch = DirectConversation.builder()
                    .teamId(TEAM_ID)
                    .conversationType(ConversationType.ONE_ON_ONE)
                    .participantIds(UUID.randomUUID() + "," + UUID.randomUUID())
                    .build();
            fakeMatch.setId(UUID.randomUUID());
            fakeMatch.setCreatedAt(NOW);

            when(directConversationRepository
                    .findByTeamIdAndParticipantIdsContainingOrderByLastMessageAtDesc(
                            TEAM_ID, USER_ID.toString()))
                    .thenReturn(List.of(conversation, fakeMatch));
            when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
            when(readReceiptRepository.findByChannelIdAndUserId(any(UUID.class), eq(USER_ID)))
                    .thenReturn(Optional.empty());
            when(directMessageRepository.countByConversationIdAndSenderIdNotAndIsDeletedFalse(
                    any(UUID.class), eq(USER_ID))).thenReturn(0L);

            List<DirectConversationSummaryResponse> result =
                    directMessageService.getConversations(TEAM_ID, USER_ID);

            // Only the real conversation should be returned, fakeMatch is filtered out
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(CONVERSATION_ID);
        }

        @Test
        void getConversations_sortsByLastMessageDesc() {
            DirectConversation older = DirectConversation.builder()
                    .teamId(TEAM_ID)
                    .conversationType(ConversationType.ONE_ON_ONE)
                    .participantIds(sortedParticipantIds)
                    .lastMessageAt(NOW.minusSeconds(600))
                    .build();
            older.setId(UUID.randomUUID());
            older.setCreatedAt(NOW);

            DirectConversation newer = DirectConversation.builder()
                    .teamId(TEAM_ID)
                    .conversationType(ConversationType.ONE_ON_ONE)
                    .participantIds(sortedParticipantIds)
                    .lastMessageAt(NOW)
                    .build();
            newer.setId(UUID.randomUUID());
            newer.setCreatedAt(NOW);

            when(directConversationRepository
                    .findByTeamIdAndParticipantIdsContainingOrderByLastMessageAtDesc(
                            TEAM_ID, USER_ID.toString()))
                    .thenReturn(List.of(older, newer));
            when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
            when(readReceiptRepository.findByChannelIdAndUserId(any(UUID.class), eq(USER_ID)))
                    .thenReturn(Optional.empty());
            when(directMessageRepository.countByConversationIdAndSenderIdNotAndIsDeletedFalse(
                    any(UUID.class), eq(USER_ID))).thenReturn(0L);

            List<DirectConversationSummaryResponse> result =
                    directMessageService.getConversations(TEAM_ID, USER_ID);

            assertThat(result).hasSize(2);
            // Newer should come first
            assertThat(result.get(0).id()).isEqualTo(newer.getId());
            assertThat(result.get(1).id()).isEqualTo(older.getId());
        }

        @Test
        void getConversations_populatesUnreadAndPreview() {
            conversation.setLastMessagePreview("Hello there!");
            conversation.setLastMessageAt(NOW);

            ReadReceipt receipt = ReadReceipt.builder()
                    .channelId(CONVERSATION_ID)
                    .userId(USER_ID)
                    .lastReadAt(NOW.minusSeconds(300))
                    .lastReadMessageId(UUID.randomUUID())
                    .build();

            when(directConversationRepository
                    .findByTeamIdAndParticipantIdsContainingOrderByLastMessageAtDesc(
                            TEAM_ID, USER_ID.toString()))
                    .thenReturn(List.of(conversation));
            when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(testUser));
            when(readReceiptRepository.findByChannelIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(receipt));
            when(directMessageRepository
                    .countByConversationIdAndSenderIdNotAndCreatedAtAfterAndIsDeletedFalse(
                            CONVERSATION_ID, USER_ID, receipt.getLastReadAt()))
                    .thenReturn(3L);

            List<DirectConversationSummaryResponse> result =
                    directMessageService.getConversations(TEAM_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).unreadCount()).isEqualTo(3);
            assertThat(result.get(0).lastMessagePreview()).isEqualTo("Hello there!");
        }

        @Test
        void deleteConversation_success() {
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            directMessageService.deleteConversation(CONVERSATION_ID, USER_ID);

            verify(directMessageRepository).deleteByConversationId(CONVERSATION_ID);
            verify(readReceiptRepository).deleteByChannelId(CONVERSATION_ID);
            verify(directConversationRepository).delete(conversation);
        }

        @Test
        void deleteConversation_notParticipant_throwsAuth() {
            UUID outsider = UUID.randomUUID();
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> directMessageService.deleteConversation(
                    CONVERSATION_ID, outsider))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ── Message Operations ────────────────────────────────────────────────

    @Nested
    class MessageTests {

        @Test
        void sendDirectMessage_success() {
            var request = new SendDirectMessageRequest("Hello!");
            DirectMessage entity = buildDirectMessage(MESSAGE_ID, "Hello!");

            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));
            when(directMessageMapper.toEntity(request)).thenReturn(entity);
            when(directMessageRepository.save(any(DirectMessage.class))).thenAnswer(inv -> {
                DirectMessage m = inv.getArgument(0);
                m.setId(MESSAGE_ID);
                m.setCreatedAt(NOW);
                m.setUpdatedAt(NOW);
                return m;
            });
            when(directConversationRepository.save(any(DirectConversation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            DirectMessageResponse result = directMessageService.sendDirectMessage(
                    CONVERSATION_ID, request, USER_ID);

            assertThat(result.content()).isEqualTo("Hello!");
            assertThat(result.senderDisplayName()).isEqualTo("Test User");
            verify(directConversationRepository).save(any(DirectConversation.class));
        }

        @Test
        void sendDirectMessage_notParticipant_throwsAuth() {
            UUID outsider = UUID.randomUUID();
            var request = new SendDirectMessageRequest("Hello!");
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> directMessageService.sendDirectMessage(
                    CONVERSATION_ID, request, outsider))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void sendDirectMessage_tooLong_throwsValidation() {
            var request = new SendDirectMessageRequest("x".repeat(10001));
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> directMessageService.sendDirectMessage(
                    CONVERSATION_ID, request, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum length");
        }

        @Test
        void getMessages_paginatedNewestFirst() {
            DirectMessage m1 = buildDirectMessage(UUID.randomUUID(), "Msg 1");
            DirectMessage m2 = buildDirectMessage(UUID.randomUUID(), "Msg 2");
            Page<DirectMessage> page = new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 10), 2);

            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));
            when(directMessageRepository
                    .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                            eq(CONVERSATION_ID), any(Pageable.class)))
                    .thenReturn(page);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            PageResponse<DirectMessageResponse> result = directMessageService.getMessages(
                    CONVERSATION_ID, USER_ID, 0, 10);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        void getMessages_notParticipant_throwsAuth() {
            UUID outsider = UUID.randomUUID();
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));

            assertThatThrownBy(() -> directMessageService.getMessages(
                    CONVERSATION_ID, outsider, 0, 10))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void editDirectMessage_bySender_success() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "Original");
            message.setSenderId(USER_ID);
            var request = new UpdateDirectMessageRequest("Updated");

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            DirectMessageResponse result = directMessageService.editDirectMessage(
                    MESSAGE_ID, request, USER_ID);

            assertThat(result.content()).isEqualTo("Updated");
            assertThat(result.isEdited()).isTrue();
        }

        @Test
        void editDirectMessage_byOtherUser_throwsAuth() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "Original");
            message.setSenderId(OTHER_USER_ID);
            var request = new UpdateDirectMessageRequest("Updated");

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            assertThatThrownBy(() -> directMessageService.editDirectMessage(
                    MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("sender");
        }

        @Test
        void editDirectMessage_deletedMessage_throwsValidation() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "Deleted");
            message.setSenderId(USER_ID);
            message.setDeleted(true);
            var request = new UpdateDirectMessageRequest("Updated");

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            assertThatThrownBy(() -> directMessageService.editDirectMessage(
                    MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("deleted");
        }

        @Test
        void editDirectMessage_setsEditedFlag() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "Original");
            message.setSenderId(USER_ID);
            var request = new UpdateDirectMessageRequest("Edited");

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            DirectMessageResponse result = directMessageService.editDirectMessage(
                    MESSAGE_ID, request, USER_ID);

            assertThat(result.isEdited()).isTrue();
            assertThat(result.editedAt()).isNotNull();
        }

        @Test
        void deleteDirectMessage_bySender_success() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "To delete");
            message.setSenderId(USER_ID);

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            directMessageService.deleteDirectMessage(MESSAGE_ID, USER_ID);

            ArgumentCaptor<DirectMessage> captor = ArgumentCaptor.forClass(DirectMessage.class);
            verify(directMessageRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
            assertThat(captor.getValue().getContent()).isEqualTo("This message was deleted");
        }

        @Test
        void deleteDirectMessage_byOtherUser_throwsAuth() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "Not mine");
            message.setSenderId(OTHER_USER_ID);

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            assertThatThrownBy(() -> directMessageService.deleteDirectMessage(
                    MESSAGE_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void deleteDirectMessage_softDeletes() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "Original content");
            message.setSenderId(USER_ID);

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            directMessageService.deleteDirectMessage(MESSAGE_ID, USER_ID);

            ArgumentCaptor<DirectMessage> captor = ArgumentCaptor.forClass(DirectMessage.class);
            verify(directMessageRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
        }
    }

    // ── Read Tracking ─────────────────────────────────────────────────────

    @Nested
    class ReadTrackingTests {

        @Test
        void markConversationRead_success() {
            when(directConversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(conversation));
            when(readReceiptRepository.findByChannelIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(readReceiptRepository.save(any(ReadReceipt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            directMessageService.markConversationRead(CONVERSATION_ID, USER_ID);

            ArgumentCaptor<ReadReceipt> captor = ArgumentCaptor.forClass(ReadReceipt.class);
            verify(readReceiptRepository).save(captor.capture());
            assertThat(captor.getValue().getChannelId()).isEqualTo(CONVERSATION_ID);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getLastReadAt()).isNotNull();
        }

        @Test
        void getUnreadCount_withUnreadMessages() {
            ReadReceipt receipt = ReadReceipt.builder()
                    .channelId(CONVERSATION_ID)
                    .userId(USER_ID)
                    .lastReadAt(NOW.minusSeconds(300))
                    .lastReadMessageId(UUID.randomUUID())
                    .build();

            when(readReceiptRepository.findByChannelIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(receipt));
            when(directMessageRepository
                    .countByConversationIdAndSenderIdNotAndCreatedAtAfterAndIsDeletedFalse(
                            CONVERSATION_ID, USER_ID, receipt.getLastReadAt()))
                    .thenReturn(5L);

            long result = directMessageService.getUnreadCount(CONVERSATION_ID, USER_ID);

            assertThat(result).isEqualTo(5);
        }

        @Test
        void getUnreadCount_allRead_returnsZero() {
            ReadReceipt receipt = ReadReceipt.builder()
                    .channelId(CONVERSATION_ID)
                    .userId(USER_ID)
                    .lastReadAt(NOW)
                    .lastReadMessageId(UUID.randomUUID())
                    .build();

            when(readReceiptRepository.findByChannelIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.of(receipt));
            when(directMessageRepository
                    .countByConversationIdAndSenderIdNotAndCreatedAtAfterAndIsDeletedFalse(
                            CONVERSATION_ID, USER_ID, receipt.getLastReadAt()))
                    .thenReturn(0L);

            long result = directMessageService.getUnreadCount(CONVERSATION_ID, USER_ID);

            assertThat(result).isEqualTo(0);
        }

        @Test
        void getUnreadCount_noReadReceipt_countsAll() {
            when(readReceiptRepository.findByChannelIdAndUserId(CONVERSATION_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(directMessageRepository.countByConversationIdAndSenderIdNotAndIsDeletedFalse(
                    CONVERSATION_ID, USER_ID))
                    .thenReturn(10L);

            long result = directMessageService.getUnreadCount(CONVERSATION_ID, USER_ID);

            assertThat(result).isEqualTo(10);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    @Nested
    class HelperTests {

        @Test
        void buildParticipantIds_consistentOrdering() {
            String result1 = DirectMessageService.buildParticipantIds(USER_ID, OTHER_USER_ID);
            String result2 = DirectMessageService.buildParticipantIds(OTHER_USER_ID, USER_ID);
            assertThat(result1).isEqualTo(result2);
            assertThat(result1).contains(",");
        }

        @Test
        void resolveDisplayName_withDisplayName() {
            DirectMessage message = buildDirectMessage(MESSAGE_ID, "test");
            message.setSenderId(USER_ID);

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            // Use editDirectMessage to trigger resolveDisplayName
            var request = new UpdateDirectMessageRequest("updated");
            DirectMessageResponse result = directMessageService.editDirectMessage(
                    MESSAGE_ID, request, USER_ID);

            assertThat(result.senderDisplayName()).isEqualTo("Test User");
        }

        @Test
        void resolveDisplayName_fallsBackToEmail() {
            User noDisplayName = new User();
            noDisplayName.setId(USER_ID);
            noDisplayName.setDisplayName(null);
            noDisplayName.setEmail("fallback@example.com");

            DirectMessage message = buildDirectMessage(MESSAGE_ID, "test");
            message.setSenderId(USER_ID);

            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(noDisplayName));

            // Use editDirectMessage to trigger resolveDisplayName
            var request = new UpdateDirectMessageRequest("updated");
            DirectMessageResponse result = directMessageService.editDirectMessage(
                    MESSAGE_ID, request, USER_ID);

            assertThat(result.senderDisplayName()).isEqualTo("fallback@example.com");
        }
    }

    // ── Test Data Builders ────────────────────────────────────────────────

    private DirectMessage buildDirectMessage(UUID id, String content) {
        DirectMessage msg = DirectMessage.builder()
                .conversationId(CONVERSATION_ID)
                .senderId(USER_ID)
                .content(content)
                .messageType(MessageType.TEXT)
                .build();
        msg.setId(id);
        msg.setCreatedAt(NOW);
        msg.setUpdatedAt(NOW);
        return msg;
    }
}
