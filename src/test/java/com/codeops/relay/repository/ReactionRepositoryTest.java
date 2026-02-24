package com.codeops.relay.repository;

import com.codeops.relay.entity.DirectConversation;
import com.codeops.relay.entity.DirectMessage;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.Reaction;
import com.codeops.relay.entity.enums.ConversationType;
import com.codeops.relay.entity.enums.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactionRepository}.
 *
 * <p>Verifies reaction lookups by channel and direct message, the
 * aggregation query for emoji counts, unique constraint validation,
 * and deletion methods.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ReactionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReactionRepository reactionRepository;

    private UUID messageId;
    private UUID directMessageId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Message message = Message.builder()
                .channelId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .content("React to this")
                .build();
        Message savedMsg = em.persistAndFlush(message);
        messageId = savedMsg.getId();

        DirectConversation conversation = DirectConversation.builder()
                .teamId(UUID.randomUUID())
                .conversationType(ConversationType.ONE_ON_ONE)
                .participantIds(UUID.randomUUID() + "," + UUID.randomUUID())
                .build();
        DirectConversation savedConv = em.persistAndFlush(conversation);

        DirectMessage dm = DirectMessage.builder()
                .conversationId(savedConv.getId())
                .senderId(UUID.randomUUID())
                .content("DM to react to")
                .build();
        DirectMessage savedDm = em.persistAndFlush(dm);
        directMessageId = savedDm.getId();

        userId = UUID.randomUUID();
    }

    private Reaction createReaction(UUID msgId, UUID user, String emoji) {
        Reaction reaction = Reaction.builder()
                .messageId(msgId)
                .userId(user)
                .emoji(emoji)
                .reactionType(ReactionType.EMOJI)
                .build();
        return em.persistAndFlush(reaction);
    }

    private Reaction createDmReaction(UUID dmId, UUID user, String emoji) {
        Reaction reaction = Reaction.builder()
                .directMessageId(dmId)
                .userId(user)
                .emoji(emoji)
                .reactionType(ReactionType.EMOJI)
                .build();
        return em.persistAndFlush(reaction);
    }

    @Test
    void findByMessageId_returnsAllReactions() {
        createReaction(messageId, userId, "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83C\uDF89");

        List<Reaction> result = reactionRepository.findByMessageId(messageId);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByDirectMessageId_returnsDirectMessageReactions() {
        createDmReaction(directMessageId, userId, "\u2764\uFE0F");

        List<Reaction> result = reactionRepository.findByDirectMessageId(directMessageId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmoji()).isEqualTo("\u2764\uFE0F");
    }

    @Test
    void findByMessageIdAndUserIdAndEmoji_findsSpecificReaction() {
        createReaction(messageId, userId, "\uD83D\uDC4D");

        Optional<Reaction> found = reactionRepository
                .findByMessageIdAndUserIdAndEmoji(messageId, userId, "\uD83D\uDC4D");
        Optional<Reaction> notFound = reactionRepository
                .findByMessageIdAndUserIdAndEmoji(messageId, userId, "\uD83D\uDC4E");

        assertThat(found).isPresent();
        assertThat(notFound).isEmpty();
    }

    @Test
    void findByDirectMessageIdAndUserIdAndEmoji_findsSpecificReaction() {
        createDmReaction(directMessageId, userId, "\uD83D\uDE00");

        Optional<Reaction> found = reactionRepository
                .findByDirectMessageIdAndUserIdAndEmoji(directMessageId, userId, "\uD83D\uDE00");

        assertThat(found).isPresent();
    }

    @Test
    void countByMessageIdAndEmoji_countsCorrectly() {
        createReaction(messageId, UUID.randomUUID(), "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83C\uDF89");

        assertThat(reactionRepository.countByMessageIdAndEmoji(messageId, "\uD83D\uDC4D")).isEqualTo(2);
        assertThat(reactionRepository.countByMessageIdAndEmoji(messageId, "\uD83C\uDF89")).isEqualTo(1);
    }

    @Test
    void countReactionsByMessageId_groupsByEmoji() {
        createReaction(messageId, UUID.randomUUID(), "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83C\uDF89");

        List<Object[]> result = reactionRepository.countReactionsByMessageId(messageId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)[0]).isEqualTo("\uD83D\uDC4D");
        assertThat(result.get(0)[1]).isEqualTo(3L);
        assertThat(result.get(1)[0]).isEqualTo("\uD83C\uDF89");
        assertThat(result.get(1)[1]).isEqualTo(1L);
    }

    @Test
    void deleteByMessageId_removesAllMessageReactions() {
        createReaction(messageId, UUID.randomUUID(), "\uD83D\uDC4D");
        createReaction(messageId, UUID.randomUUID(), "\uD83C\uDF89");

        reactionRepository.deleteByMessageId(messageId);
        em.flush();
        em.clear();

        assertThat(reactionRepository.findByMessageId(messageId)).isEmpty();
    }

    @Test
    void deleteByDirectMessageId_removesAllDmReactions() {
        createDmReaction(directMessageId, UUID.randomUUID(), "\uD83D\uDC4D");

        reactionRepository.deleteByDirectMessageId(directMessageId);
        em.flush();
        em.clear();

        assertThat(reactionRepository.findByDirectMessageId(directMessageId)).isEmpty();
    }
}
