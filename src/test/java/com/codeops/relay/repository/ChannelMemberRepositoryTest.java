package com.codeops.relay.repository;

import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.ChannelMember;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChannelMemberRepository}.
 *
 * <p>Verifies member lookups by channel and user, the custom JPQL query
 * for channel IDs, exists/count methods, mute filtering, and deletion.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ChannelMemberRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    private UUID channelId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Channel channel = Channel.builder()
                .name("Test Channel")
                .slug("test-channel")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        Channel saved = em.persistAndFlush(channel);
        channelId = saved.getId();
        userId = UUID.randomUUID();
    }

    private ChannelMember createMember(UUID channel, UUID user, MemberRole role, boolean muted) {
        ChannelMember member = ChannelMember.builder()
                .channelId(channel)
                .userId(user)
                .role(role)
                .joinedAt(Instant.now())
                .isMuted(muted)
                .build();
        return em.persistAndFlush(member);
    }

    @Test
    void findByChannelId_returnsAllMembers() {
        createMember(channelId, userId, MemberRole.OWNER, false);
        createMember(channelId, UUID.randomUUID(), MemberRole.MEMBER, false);

        List<ChannelMember> result = channelMemberRepository.findByChannelId(channelId);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByUserId_returnsAllMemberships() {
        Channel channel2 = Channel.builder()
                .name("Second Channel")
                .slug("second-channel")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        Channel saved2 = em.persistAndFlush(channel2);

        createMember(channelId, userId, MemberRole.MEMBER, false);
        createMember(saved2.getId(), userId, MemberRole.ADMIN, false);

        List<ChannelMember> result = channelMemberRepository.findByUserId(userId);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByUserIdAndIsMutedFalse_excludesMuted() {
        Channel channel2 = Channel.builder()
                .name("Muted Channel")
                .slug("muted-channel")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        Channel saved2 = em.persistAndFlush(channel2);

        createMember(channelId, userId, MemberRole.MEMBER, false);
        createMember(saved2.getId(), userId, MemberRole.MEMBER, true);

        List<ChannelMember> result = channelMemberRepository.findByUserIdAndIsMutedFalse(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChannelId()).isEqualTo(channelId);
    }

    @Test
    void findByChannelIdAndUserId_returnsSpecificMembership() {
        createMember(channelId, userId, MemberRole.OWNER, false);

        Optional<ChannelMember> found = channelMemberRepository
                .findByChannelIdAndUserId(channelId, userId);
        Optional<ChannelMember> notFound = channelMemberRepository
                .findByChannelIdAndUserId(channelId, UUID.randomUUID());

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(MemberRole.OWNER);
        assertThat(notFound).isEmpty();
    }

    @Test
    void existsByChannelIdAndUserId_checksCorrectly() {
        createMember(channelId, userId, MemberRole.MEMBER, false);

        assertThat(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).isTrue();
        assertThat(channelMemberRepository.existsByChannelIdAndUserId(channelId, UUID.randomUUID())).isFalse();
    }

    @Test
    void countByChannelId_returnsCorrectCount() {
        createMember(channelId, UUID.randomUUID(), MemberRole.OWNER, false);
        createMember(channelId, UUID.randomUUID(), MemberRole.MEMBER, false);
        createMember(channelId, UUID.randomUUID(), MemberRole.MEMBER, false);

        assertThat(channelMemberRepository.countByChannelId(channelId)).isEqualTo(3);
    }

    @Test
    void deleteByChannelIdAndUserId_removesSpecificMembership() {
        createMember(channelId, userId, MemberRole.MEMBER, false);
        UUID otherUser = UUID.randomUUID();
        createMember(channelId, otherUser, MemberRole.MEMBER, false);

        channelMemberRepository.deleteByChannelIdAndUserId(channelId, userId);
        em.flush();
        em.clear();

        assertThat(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).isFalse();
        assertThat(channelMemberRepository.existsByChannelIdAndUserId(channelId, otherUser)).isTrue();
    }

    @Test
    void findChannelIdsByUserId_returnsChannelUuids() {
        Channel channel2 = Channel.builder()
                .name("Channel 2")
                .slug("channel-2")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        Channel saved2 = em.persistAndFlush(channel2);

        createMember(channelId, userId, MemberRole.MEMBER, false);
        createMember(saved2.getId(), userId, MemberRole.ADMIN, false);

        List<UUID> channelIds = channelMemberRepository.findChannelIdsByUserId(userId);

        assertThat(channelIds).hasSize(2);
        assertThat(channelIds).containsExactlyInAnyOrder(channelId, saved2.getId());
    }
}
