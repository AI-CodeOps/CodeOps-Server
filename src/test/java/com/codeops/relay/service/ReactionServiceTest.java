package com.codeops.relay.service;

import com.codeops.entity.User;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.ReactionMapper;
import com.codeops.relay.dto.request.AddReactionRequest;
import com.codeops.relay.dto.response.ReactionResponse;
import com.codeops.relay.dto.response.ReactionSummaryResponse;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.Reaction;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.entity.enums.ReactionType;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.ReactionRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReactionService}.
 *
 * <p>Covers toggle add/remove, aggregated summaries, per-user queries,
 * and bulk deletion.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock private ReactionRepository reactionRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChannelMemberRepository channelMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReactionMapper reactionMapper;

    @InjectMocks private ReactionService reactionService;

    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_2_ID = UUID.randomUUID();
    private static final UUID USER_3_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private Message message;
    private User testUser;

    @BeforeEach
    void setUp() {
        message = Message.builder()
                .channelId(CHANNEL_ID)
                .senderId(USER_ID)
                .content("Hello")
                .messageType(MessageType.TEXT)
                .build();
        message.setId(MESSAGE_ID);
        message.setCreatedAt(NOW);
        message.setUpdatedAt(NOW);

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
    }

    // ── Toggle ───────────────────────────────────────────────────────────

    @Nested
    class ToggleTests {

        @Test
        void toggleReaction_addsNew() {
            var request = new AddReactionRequest("\uD83D\uDC4D");
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(reactionRepository.findByMessageIdAndUserIdAndEmoji(MESSAGE_ID, USER_ID, "\uD83D\uDC4D"))
                    .thenReturn(Optional.empty());
            when(reactionRepository.save(any(Reaction.class))).thenAnswer(inv -> {
                Reaction r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                r.setCreatedAt(NOW);
                return r;
            });
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            ReactionResponse result = reactionService.toggleReaction(MESSAGE_ID, request, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.emoji()).isEqualTo("\uD83D\uDC4D");
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.userDisplayName()).isEqualTo("Test User");
            assertThat(result.reactionType()).isEqualTo(ReactionType.EMOJI);
            verify(reactionRepository).save(any(Reaction.class));
        }

        @Test
        void toggleReaction_removesExisting() {
            var request = new AddReactionRequest("\uD83D\uDC4D");
            Reaction existing = Reaction.builder()
                    .messageId(MESSAGE_ID).userId(USER_ID).emoji("\uD83D\uDC4D")
                    .reactionType(ReactionType.EMOJI).build();
            existing.setId(UUID.randomUUID());

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(reactionRepository.findByMessageIdAndUserIdAndEmoji(MESSAGE_ID, USER_ID, "\uD83D\uDC4D"))
                    .thenReturn(Optional.of(existing));

            ReactionResponse result = reactionService.toggleReaction(MESSAGE_ID, request, USER_ID);

            assertThat(result).isNull();
            verify(reactionRepository).delete(existing);
            verify(reactionRepository, never()).save(any(Reaction.class));
        }

        @Test
        void toggleReaction_notChannelMember_throwsAuth() {
            var request = new AddReactionRequest("\uD83D\uDC4D");
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> reactionService.toggleReaction(MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void toggleReaction_messageNotFound_throws() {
            var request = new AddReactionRequest("\uD83D\uDC4D");
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reactionService.toggleReaction(MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void toggleReaction_blankEmoji_throwsValidation() {
            var request = new AddReactionRequest("   ");
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(reactionRepository.findByMessageIdAndUserIdAndEmoji(MESSAGE_ID, USER_ID, "   "))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> reactionService.toggleReaction(MESSAGE_ID, request, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("blank");
        }
    }

    // ── Getting ──────────────────────────────────────────────────────────

    @Nested
    class GetTests {

        @Test
        void getReactionsForMessage_groupsByEmoji() {
            Reaction r1 = buildReaction(USER_ID, "\uD83D\uDC4D");
            Reaction r2 = buildReaction(USER_2_ID, "\uD83D\uDC4D");
            Reaction r3 = buildReaction(USER_3_ID, "\uD83C\uDF89");

            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(r1, r2, r3));

            List<ReactionSummaryResponse> result = reactionService.getReactionsForMessage(MESSAGE_ID);

            assertThat(result).hasSize(2);
            // thumbs up has 2, party has 1 → thumbs up first
            assertThat(result.get(0).emoji()).isEqualTo("\uD83D\uDC4D");
            assertThat(result.get(0).count()).isEqualTo(2);
            assertThat(result.get(1).emoji()).isEqualTo("\uD83C\uDF89");
            assertThat(result.get(1).count()).isEqualTo(1);
        }

        @Test
        void getReactionsForMessage_sortsByCountDesc() {
            Reaction r1 = buildReaction(USER_ID, "A");
            Reaction r2 = buildReaction(USER_2_ID, "B");
            Reaction r3 = buildReaction(USER_3_ID, "B");

            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(r1, r2, r3));

            List<ReactionSummaryResponse> result = reactionService.getReactionsForMessage(MESSAGE_ID);

            assertThat(result.get(0).emoji()).isEqualTo("B");
            assertThat(result.get(0).count()).isEqualTo(2);
            assertThat(result.get(1).emoji()).isEqualTo("A");
            assertThat(result.get(1).count()).isEqualTo(1);
        }

        @Test
        void getReactionsForMessage_messageNotFound_throws() {
            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(false);

            assertThatThrownBy(() -> reactionService.getReactionsForMessage(MESSAGE_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void getReactionsForMessageWithUser_setsCurrentUserReacted() {
            Reaction r1 = buildReaction(USER_ID, "\uD83D\uDC4D");
            Reaction r2 = buildReaction(USER_2_ID, "\uD83D\uDC4D");

            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(r1, r2));

            List<ReactionSummaryResponse> result = reactionService.getReactionsForMessageWithUser(
                    MESSAGE_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currentUserReacted()).isTrue();
            assertThat(result.get(0).userIds()).contains(USER_ID, USER_2_ID);
        }

        @Test
        void getReactionsForMessageWithUser_currentUserNotReacted() {
            Reaction r1 = buildReaction(USER_2_ID, "\uD83D\uDC4D");

            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(r1));

            List<ReactionSummaryResponse> result = reactionService.getReactionsForMessageWithUser(
                    MESSAGE_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).currentUserReacted()).isFalse();
        }

        @Test
        void getReactionsByUser_returnsForChannel() {
            Reaction r1 = buildReaction(USER_ID, "\uD83D\uDC4D");
            Reaction r2 = buildReaction(USER_ID, "\uD83C\uDF89");

            when(reactionRepository.findByUserIdAndMessageChannelId(USER_ID, CHANNEL_ID))
                    .thenReturn(List.of(r1, r2));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            List<ReactionResponse> result = reactionService.getReactionsByUser(USER_ID, CHANNEL_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).userDisplayName()).isEqualTo("Test User");
        }
    }

    // ── Bulk ─────────────────────────────────────────────────────────────

    @Nested
    class BulkTests {

        @Test
        void removeAllReactionsForMessage_success() {
            reactionService.removeAllReactionsForMessage(MESSAGE_ID);

            verify(reactionRepository).deleteByMessageId(MESSAGE_ID);
        }
    }

    // ── Test Data Builders ───────────────────────────────────────────────

    private Reaction buildReaction(UUID userId, String emoji) {
        Reaction reaction = Reaction.builder()
                .messageId(MESSAGE_ID)
                .userId(userId)
                .emoji(emoji)
                .reactionType(ReactionType.EMOJI)
                .build();
        reaction.setId(UUID.randomUUID());
        reaction.setCreatedAt(NOW);
        return reaction;
    }
}
