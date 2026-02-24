package com.codeops.relay.entity;

import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Channel} entity.
 *
 * <p>Verifies entity creation, BaseEntity lifecycle callbacks,
 * enum persistence, default values, and cascade behavior for
 * members and pinned messages.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ChannelTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void createChannel_allFields_persistsCorrectly() {
        UUID teamId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        Channel channel = Channel.builder()
                .name("General")
                .slug("general")
                .description("Main team channel")
                .topic("Welcome to the team!")
                .channelType(ChannelType.PUBLIC)
                .teamId(teamId)
                .createdBy(createdBy)
                .build();

        Channel saved = em.persistAndFlush(channel);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("General");
        assertThat(saved.getSlug()).isEqualTo("general");
        assertThat(saved.getDescription()).isEqualTo("Main team channel");
        assertThat(saved.getTopic()).isEqualTo("Welcome to the team!");
        assertThat(saved.getChannelType()).isEqualTo(ChannelType.PUBLIC);
        assertThat(saved.getTeamId()).isEqualTo(teamId);
        assertThat(saved.getCreatedBy()).isEqualTo(createdBy);
    }

    @Test
    void baseEntityLifecycle_setsCreatedAtAndUpdatedAt() {
        Channel channel = Channel.builder()
                .name("Lifecycle Test")
                .slug("lifecycle-test")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();

        Channel saved = em.persistAndFlush(channel);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void baseEntityLifecycle_updatesUpdatedAtOnModification() {
        Channel channel = Channel.builder()
                .name("Update Test")
                .slug("update-test")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();

        Channel saved = em.persistAndFlush(channel);
        Instant originalUpdatedAt = saved.getUpdatedAt();

        saved.setTopic("New topic");
        em.persistAndFlush(saved);

        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void channelType_persistsAllEnumValues() {
        UUID teamId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        for (ChannelType type : ChannelType.values()) {
            Channel channel = Channel.builder()
                    .name("Type " + type.name())
                    .slug("type-" + type.name().toLowerCase())
                    .channelType(type)
                    .teamId(teamId)
                    .createdBy(createdBy)
                    .build();

            Channel saved = em.persistAndFlush(channel);
            em.clear();

            Channel found = em.find(Channel.class, saved.getId());
            assertThat(found.getChannelType()).isEqualTo(type);
        }
    }

    @Test
    void isArchived_defaultsFalse() {
        Channel channel = Channel.builder()
                .name("Archived Test")
                .slug("archived-test")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();

        Channel saved = em.persistAndFlush(channel);

        assertThat(saved.isArchived()).isFalse();
    }

    @Test
    void projectChannel_setsProjectId() {
        UUID projectId = UUID.randomUUID();

        Channel channel = Channel.builder()
                .name("Project Channel")
                .slug("project-channel")
                .channelType(ChannelType.PROJECT)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .projectId(projectId)
                .build();

        Channel saved = em.persistAndFlush(channel);

        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getServiceId()).isNull();
    }

    @Test
    void serviceChannel_setsServiceId() {
        UUID serviceId = UUID.randomUUID();

        Channel channel = Channel.builder()
                .name("Service Channel")
                .slug("service-channel")
                .channelType(ChannelType.SERVICE)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .serviceId(serviceId)
                .build();

        Channel saved = em.persistAndFlush(channel);

        assertThat(saved.getServiceId()).isEqualTo(serviceId);
        assertThat(saved.getProjectId()).isNull();
    }

    @Test
    void membersCascade_persistsWithChannel() {
        Channel channel = Channel.builder()
                .name("Cascade Test")
                .slug("cascade-test")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();

        Channel saved = em.persistAndFlush(channel);

        ChannelMember member = ChannelMember.builder()
                .channelId(saved.getId())
                .userId(UUID.randomUUID())
                .role(MemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        saved.getMembers().add(member);
        member.setChannel(saved);

        em.persistAndFlush(saved);
        em.clear();

        Channel found = em.find(Channel.class, saved.getId());
        assertThat(found.getMembers()).hasSize(1);
        assertThat(found.getMembers().get(0).getRole()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    void pinnedMessagesCascade_persistsWithChannel() {
        Channel channel = Channel.builder()
                .name("Pin Cascade")
                .slug("pin-cascade")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();

        Channel saved = em.persistAndFlush(channel);

        PinnedMessage pin = PinnedMessage.builder()
                .messageId(UUID.randomUUID())
                .pinnedBy(UUID.randomUUID())
                .channel(saved)
                .build();
        saved.getPinnedMessages().add(pin);

        em.persistAndFlush(saved);
        em.clear();

        Channel found = em.find(Channel.class, saved.getId());
        assertThat(found.getPinnedMessages()).hasSize(1);
    }

    @Test
    void orphanRemoval_removesMemberWhenRemovedFromList() {
        Channel channel = Channel.builder()
                .name("Orphan Test")
                .slug("orphan-test")
                .channelType(ChannelType.PUBLIC)
                .teamId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();

        Channel saved = em.persistAndFlush(channel);

        ChannelMember member = ChannelMember.builder()
                .channelId(saved.getId())
                .userId(UUID.randomUUID())
                .role(MemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
        saved.getMembers().add(member);
        member.setChannel(saved);
        em.persistAndFlush(saved);

        saved.getMembers().clear();
        em.persistAndFlush(saved);
        em.clear();

        Channel found = em.find(Channel.class, saved.getId());
        assertThat(found.getMembers()).isEmpty();
    }
}
