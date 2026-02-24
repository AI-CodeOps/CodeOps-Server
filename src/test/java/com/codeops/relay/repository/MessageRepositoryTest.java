package com.codeops.relay.repository;

import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.enums.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageRepository}.
 *
 * <p>Verifies paginated channel messages, thread replies by parentId,
 * unread count queries, search within and across channels, and
 * count/delete operations.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MessageRepository messageRepository;

    private UUID channelId;
    private UUID senderId;

    @BeforeEach
    void setUp() {
        channelId = UUID.randomUUID();
        senderId = UUID.randomUUID();
    }

    private Message createMessage(UUID channel, String content) {
        Message message = Message.builder()
                .channelId(channel)
                .senderId(senderId)
                .content(content)
                .build();
        return em.persistAndFlush(message);
    }

    private Message createDeletedMessage(UUID channel, String content) {
        Message message = Message.builder()
                .channelId(channel)
                .senderId(senderId)
                .content(content)
                .isDeleted(true)
                .build();
        return em.persistAndFlush(message);
    }

    @Test
    void findByChannelIdAndIsDeletedFalse_excludesDeletedMessages() {
        createMessage(channelId, "Visible 1");
        createMessage(channelId, "Visible 2");
        createDeletedMessage(channelId, "Deleted");

        Page<Message> result = messageRepository
                .findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findTopLevelMessages_excludesThreadReplies() {
        Message parent = createMessage(channelId, "Parent message");

        Message reply = Message.builder()
                .channelId(channelId)
                .senderId(senderId)
                .content("Thread reply")
                .parentId(parent.getId())
                .build();
        em.persistAndFlush(reply);

        createMessage(channelId, "Another top-level");

        Page<Message> result = messageRepository
                .findByChannelIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).noneMatch(m -> m.getParentId() != null);
    }

    @Test
    void findByParentId_returnsThreadReplies() {
        Message parent = createMessage(channelId, "Thread root");

        for (int i = 0; i < 3; i++) {
            Message reply = Message.builder()
                    .channelId(channelId)
                    .senderId(senderId)
                    .content("Reply " + i)
                    .parentId(parent.getId())
                    .build();
            em.persistAndFlush(reply);
        }

        List<Message> replies = messageRepository
                .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parent.getId());

        assertThat(replies).hasSize(3);
    }

    @Test
    void countUnreadMessages_countsSinceTimestamp() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        createMessage(channelId, "Old message");
        createMessage(channelId, "Recent message");
        createDeletedMessage(channelId, "Deleted recent");

        long count = messageRepository.countUnreadMessages(channelId, oneHourAgo);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void searchInChannel_findsMatchingContent() {
        createMessage(channelId, "Hello team, welcome aboard!");
        createMessage(channelId, "Deployment completed successfully");
        createMessage(channelId, "Welcome to the project");

        Page<Message> result = messageRepository.searchInChannel(
                channelId, "welcome", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void searchAcrossChannels_findsAcrossMultipleChannels() {
        UUID channel2 = UUID.randomUUID();
        createMessage(channelId, "Deploy to staging");
        createMessage(channel2, "Deploy to production");
        createMessage(channelId, "Unrelated message");

        Page<Message> result = messageRepository.searchAcrossChannels(
                List.of(channelId, channel2), "deploy", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void countByChannelIdAndCreatedAtAfter_countsRecentMessages() {
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);

        createMessage(channelId, "Message 1");
        createMessage(channelId, "Message 2");
        createMessage(channelId, "Message 3");

        long count = messageRepository.countByChannelIdAndCreatedAtAfter(channelId, twoHoursAgo);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByChannelIdAndCreatedAtAfterAndIsDeletedFalse_excludesDeleted() {
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);

        createMessage(channelId, "Visible");
        createDeletedMessage(channelId, "Deleted");

        long count = messageRepository.countByChannelIdAndCreatedAtAfterAndIsDeletedFalse(
                channelId, twoHoursAgo);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteByChannelId_removesAllChannelMessages() {
        createMessage(channelId, "To delete 1");
        createMessage(channelId, "To delete 2");

        UUID otherChannel = UUID.randomUUID();
        createMessage(otherChannel, "Keep this");

        messageRepository.deleteByChannelId(channelId);
        em.flush();
        em.clear();

        assertThat(messageRepository.countByChannelIdAndCreatedAtAfter(
                channelId, Instant.EPOCH)).isZero();
        assertThat(messageRepository.countByChannelIdAndCreatedAtAfter(
                otherChannel, Instant.EPOCH)).isEqualTo(1);
    }

    @Test
    void pagination_worksCorrectly() {
        for (int i = 0; i < 10; i++) {
            createMessage(channelId, "Message " + i);
        }

        Page<Message> page1 = messageRepository
                .findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelId, PageRequest.of(0, 3));
        Page<Message> page2 = messageRepository
                .findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelId, PageRequest.of(1, 3));

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page2.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(10);
        assertThat(page1.getTotalPages()).isEqualTo(4);
    }
}
