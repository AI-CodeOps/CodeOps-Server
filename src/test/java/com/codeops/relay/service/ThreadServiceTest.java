package com.codeops.relay.service;

import com.codeops.exception.NotFoundException;
import com.codeops.relay.dto.mapper.MessageThreadMapper;
import com.codeops.relay.dto.response.MessageThreadResponse;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.MessageThread;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.MessageThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for {@link ThreadService}.
 *
 * <p>Covers thread creation on first reply, incrementation on subsequent replies,
 * participant tracking, thread retrieval, and active thread listing.</p>
 */
@ExtendWith(MockitoExtension.class)
class ThreadServiceTest {

    @Mock private MessageThreadRepository messageThreadRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageThreadMapper messageThreadMapper;

    @InjectMocks private ThreadService threadService;

    private static final UUID ROOT_MESSAGE_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID USER_1 = UUID.randomUUID();
    private static final UUID USER_2 = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    // ── onReply ───────────────────────────────────────────────────────────

    @Nested
    class OnReplyTests {

        @Test
        void onReply_createsNewThread() {
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.empty());
            when(messageThreadRepository.save(any(MessageThread.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            threadService.onReply(ROOT_MESSAGE_ID, CHANNEL_ID, USER_1);

            ArgumentCaptor<MessageThread> captor = ArgumentCaptor.forClass(MessageThread.class);
            verify(messageThreadRepository).save(captor.capture());
            MessageThread saved = captor.getValue();
            assertThat(saved.getRootMessageId()).isEqualTo(ROOT_MESSAGE_ID);
            assertThat(saved.getChannelId()).isEqualTo(CHANNEL_ID);
            assertThat(saved.getReplyCount()).isEqualTo(1);
            assertThat(saved.getLastReplyBy()).isEqualTo(USER_1);
            assertThat(saved.getLastReplyAt()).isNotNull();
            assertThat(saved.getParticipantIds()).contains(USER_1.toString());
        }

        @Test
        void onReply_incrementsExistingThread() {
            MessageThread existing = MessageThread.builder()
                    .rootMessageId(ROOT_MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(3)
                    .lastReplyAt(NOW.minusSeconds(60))
                    .lastReplyBy(USER_1)
                    .participantIds(USER_1.toString())
                    .build();
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.of(existing));
            when(messageThreadRepository.save(any(MessageThread.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            threadService.onReply(ROOT_MESSAGE_ID, CHANNEL_ID, USER_2);

            ArgumentCaptor<MessageThread> captor = ArgumentCaptor.forClass(MessageThread.class);
            verify(messageThreadRepository).save(captor.capture());
            MessageThread saved = captor.getValue();
            assertThat(saved.getReplyCount()).isEqualTo(4);
            assertThat(saved.getLastReplyBy()).isEqualTo(USER_2);
        }

        @Test
        void onReply_addsParticipant() {
            MessageThread existing = MessageThread.builder()
                    .rootMessageId(ROOT_MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(1)
                    .participantIds(USER_1.toString())
                    .build();
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.of(existing));
            when(messageThreadRepository.save(any(MessageThread.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            threadService.onReply(ROOT_MESSAGE_ID, CHANNEL_ID, USER_2);

            ArgumentCaptor<MessageThread> captor = ArgumentCaptor.forClass(MessageThread.class);
            verify(messageThreadRepository).save(captor.capture());
            assertThat(captor.getValue().getParticipantIds())
                    .contains(USER_1.toString())
                    .contains(USER_2.toString());
        }

        @Test
        void onReply_duplicateParticipant_noOp() {
            MessageThread existing = MessageThread.builder()
                    .rootMessageId(ROOT_MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(1)
                    .participantIds(USER_1.toString())
                    .build();
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.of(existing));
            when(messageThreadRepository.save(any(MessageThread.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            threadService.onReply(ROOT_MESSAGE_ID, CHANNEL_ID, USER_1);

            ArgumentCaptor<MessageThread> captor = ArgumentCaptor.forClass(MessageThread.class);
            verify(messageThreadRepository).save(captor.capture());
            String participants = captor.getValue().getParticipantIds();
            // Count occurrences: should only appear once
            long count = participants.chars().filter(c -> c == ',').count();
            assertThat(count).isEqualTo(0); // No commas = single participant
        }
    }

    // ── getThreadInfo ─────────────────────────────────────────────────────

    @Nested
    class GetThreadInfoTests {

        @Test
        void getThreadInfo_exists() {
            MessageThread thread = MessageThread.builder()
                    .rootMessageId(ROOT_MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(5)
                    .build();
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.of(thread));

            Optional<MessageThread> result = threadService.getThreadInfo(ROOT_MESSAGE_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getReplyCount()).isEqualTo(5);
        }

        @Test
        void getThreadInfo_notExists() {
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.empty());

            Optional<MessageThread> result = threadService.getThreadInfo(ROOT_MESSAGE_ID);

            assertThat(result).isEmpty();
        }
    }

    // ── getThread ─────────────────────────────────────────────────────────

    @Nested
    class GetThreadTests {

        @Test
        void getThread_success() {
            MessageThread thread = MessageThread.builder()
                    .rootMessageId(ROOT_MESSAGE_ID)
                    .channelId(CHANNEL_ID)
                    .replyCount(2)
                    .lastReplyAt(NOW)
                    .lastReplyBy(USER_2)
                    .participantIds(USER_1 + "," + USER_2)
                    .build();
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.of(thread));

            Message reply1 = buildMessage(UUID.randomUUID(), "Reply 1");
            Message reply2 = buildMessage(UUID.randomUUID(), "Reply 2");
            when(messageRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(ROOT_MESSAGE_ID))
                    .thenReturn(List.of(reply1, reply2));

            MessageThreadResponse result = threadService.getThread(ROOT_MESSAGE_ID, USER_1);

            assertThat(result.replyCount()).isEqualTo(2);
            assertThat(result.participantIds()).hasSize(2);
            assertThat(result.replies()).hasSize(2);
            assertThat(result.channelId()).isEqualTo(CHANNEL_ID);
        }

        @Test
        void getThread_notFound_throws() {
            when(messageThreadRepository.findByRootMessageId(ROOT_MESSAGE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadService.getThread(ROOT_MESSAGE_ID, USER_1))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── getActiveThreads ──────────────────────────────────────────────────

    @Nested
    class GetActiveThreadsTests {

        @Test
        void getActiveThreads_orderedByLastReply() {
            MessageThread t1 = MessageThread.builder()
                    .rootMessageId(UUID.randomUUID())
                    .channelId(CHANNEL_ID)
                    .replyCount(5)
                    .lastReplyAt(NOW)
                    .lastReplyBy(USER_1)
                    .participantIds(USER_1.toString())
                    .build();
            MessageThread t2 = MessageThread.builder()
                    .rootMessageId(UUID.randomUUID())
                    .channelId(CHANNEL_ID)
                    .replyCount(2)
                    .lastReplyAt(NOW.minusSeconds(300))
                    .lastReplyBy(USER_2)
                    .participantIds(USER_2.toString())
                    .build();
            when(messageThreadRepository.findByChannelIdOrderByLastReplyAtDesc(CHANNEL_ID))
                    .thenReturn(List.of(t1, t2));

            List<MessageThreadResponse> result = threadService.getActiveThreads(CHANNEL_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).replyCount()).isEqualTo(5);
            assertThat(result.get(1).replyCount()).isEqualTo(2);
            assertThat(result.get(0).replies()).isEmpty();
            assertThat(result.get(1).replies()).isEmpty();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Message buildMessage(UUID id, String content) {
        Message msg = Message.builder()
                .channelId(CHANNEL_ID)
                .senderId(USER_1)
                .content(content)
                .messageType(MessageType.TEXT)
                .build();
        msg.setId(id);
        msg.setCreatedAt(NOW);
        msg.setUpdatedAt(NOW);
        return msg;
    }
}
