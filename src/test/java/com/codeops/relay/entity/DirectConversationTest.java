package com.codeops.relay.entity;

import com.codeops.relay.entity.enums.ConversationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link DirectConversation} entity.
 *
 * <p>Verifies conversation creation for both ONE_ON_ONE and GROUP types,
 * participant ID sorting, last message tracking, and enum persistence.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class DirectConversationTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void createOneOnOneConversation_persistsCorrectly() {
        UUID teamId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        String participants = sortedParticipants(user1, user2);

        DirectConversation conversation = DirectConversation.builder()
                .teamId(teamId)
                .conversationType(ConversationType.ONE_ON_ONE)
                .participantIds(participants)
                .build();

        DirectConversation saved = em.persistAndFlush(conversation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTeamId()).isEqualTo(teamId);
        assertThat(saved.getConversationType()).isEqualTo(ConversationType.ONE_ON_ONE);
        assertThat(saved.getParticipantIds()).isEqualTo(participants);
        assertThat(saved.getName()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createGroupConversation_persistsWithName() {
        UUID teamId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();
        String participants = sortedParticipants(user1, user2, user3);

        DirectConversation conversation = DirectConversation.builder()
                .teamId(teamId)
                .conversationType(ConversationType.GROUP)
                .name("Project Team")
                .participantIds(participants)
                .build();

        DirectConversation saved = em.persistAndFlush(conversation);

        assertThat(saved.getConversationType()).isEqualTo(ConversationType.GROUP);
        assertThat(saved.getName()).isEqualTo("Project Team");
        assertThat(saved.getParticipantIds()).contains(",");
    }

    @Test
    void participantIds_sortedForConsistentLookup() {
        UUID a = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
        String sorted = sortedParticipants(b, a);

        assertThat(sorted).startsWith(a.toString());
    }

    @Test
    void lastMessageTracking_updatesCorrectly() {
        DirectConversation conversation = DirectConversation.builder()
                .teamId(UUID.randomUUID())
                .conversationType(ConversationType.ONE_ON_ONE)
                .participantIds(sortedParticipants(UUID.randomUUID(), UUID.randomUUID()))
                .build();

        DirectConversation saved = em.persistAndFlush(conversation);
        assertThat(saved.getLastMessageAt()).isNull();
        assertThat(saved.getLastMessagePreview()).isNull();

        Instant now = Instant.now();
        saved.setLastMessageAt(now);
        saved.setLastMessagePreview("Hey, how's it going?");
        em.persistAndFlush(saved);
        em.clear();

        DirectConversation found = em.find(DirectConversation.class, saved.getId());
        assertThat(found.getLastMessageAt()).isEqualTo(now);
        assertThat(found.getLastMessagePreview()).isEqualTo("Hey, how's it going?");
    }

    @Test
    void conversationType_persistsAllEnumValues() {
        for (ConversationType type : ConversationType.values()) {
            DirectConversation conversation = DirectConversation.builder()
                    .teamId(UUID.randomUUID())
                    .conversationType(type)
                    .participantIds(sortedParticipants(UUID.randomUUID(), UUID.randomUUID()))
                    .build();

            DirectConversation saved = em.persistAndFlush(conversation);
            em.clear();

            DirectConversation found = em.find(DirectConversation.class, saved.getId());
            assertThat(found.getConversationType()).isEqualTo(type);
        }
    }

    private String sortedParticipants(UUID... ids) {
        String[] strings = Arrays.stream(ids).map(UUID::toString).sorted().toArray(String[]::new);
        return String.join(",", strings);
    }
}
