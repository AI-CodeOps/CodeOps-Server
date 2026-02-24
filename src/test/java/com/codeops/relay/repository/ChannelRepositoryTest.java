package com.codeops.relay.repository;

import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.enums.ChannelType;
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
 * Tests for {@link ChannelRepository}.
 *
 * <p>Verifies team-scoped queries, type filtering, slug lookups,
 * project/service channel queries, exists/count methods, and
 * archived channel filtering.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ChannelRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChannelRepository channelRepository;

    private UUID teamId;
    private UUID otherTeamId;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        otherTeamId = UUID.randomUUID();
    }

    private Channel createChannel(UUID team, String name, String slug, ChannelType type) {
        Channel channel = Channel.builder()
                .name(name)
                .slug(slug)
                .channelType(type)
                .teamId(team)
                .createdBy(UUID.randomUUID())
                .build();
        return em.persistAndFlush(channel);
    }

    @Test
    void findByTeamId_returnsOnlyTeamChannels() {
        createChannel(teamId, "General", "general", ChannelType.PUBLIC);
        createChannel(teamId, "Random", "random", ChannelType.PUBLIC);
        createChannel(otherTeamId, "Other", "other", ChannelType.PUBLIC);

        List<Channel> result = channelRepository.findByTeamId(teamId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Channel::getTeamId).containsOnly(teamId);
    }

    @Test
    void findByTeamIdAndChannelType_filtersCorrectly() {
        createChannel(teamId, "Public", "public", ChannelType.PUBLIC);
        createChannel(teamId, "Private", "private", ChannelType.PRIVATE);
        createChannel(teamId, "Project", "project", ChannelType.PROJECT);

        List<Channel> publics = channelRepository.findByTeamIdAndChannelType(teamId, ChannelType.PUBLIC);
        List<Channel> privates = channelRepository.findByTeamIdAndChannelType(teamId, ChannelType.PRIVATE);

        assertThat(publics).hasSize(1);
        assertThat(privates).hasSize(1);
    }

    @Test
    void findByTeamIdAndIsArchivedFalse_excludesArchived() {
        Channel active = createChannel(teamId, "Active", "active", ChannelType.PUBLIC);
        Channel archived = createChannel(teamId, "Archived", "archived", ChannelType.PUBLIC);
        archived.setArchived(true);
        em.persistAndFlush(archived);

        List<Channel> result = channelRepository.findByTeamIdAndIsArchivedFalse(teamId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(active.getId());
    }

    @Test
    void findByTeamIdAndSlug_returnsMatchingChannel() {
        createChannel(teamId, "General", "general", ChannelType.PUBLIC);

        Optional<Channel> found = channelRepository.findByTeamIdAndSlug(teamId, "general");
        Optional<Channel> notFound = channelRepository.findByTeamIdAndSlug(teamId, "nonexistent");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("General");
        assertThat(notFound).isEmpty();
    }

    @Test
    void findByTeamIdAndProjectId_returnsProjectChannel() {
        UUID projectId = UUID.randomUUID();
        Channel channel = createChannel(teamId, "Project API", "project-api", ChannelType.PROJECT);
        channel.setProjectId(projectId);
        em.persistAndFlush(channel);

        Optional<Channel> found = channelRepository.findByTeamIdAndProjectId(teamId, projectId);

        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo(projectId);
    }

    @Test
    void findByTeamIdAndServiceId_returnsServiceChannel() {
        UUID serviceId = UUID.randomUUID();
        Channel channel = createChannel(teamId, "Service Redis", "service-redis", ChannelType.SERVICE);
        channel.setServiceId(serviceId);
        em.persistAndFlush(channel);

        Optional<Channel> found = channelRepository.findByTeamIdAndServiceId(teamId, serviceId);

        assertThat(found).isPresent();
        assertThat(found.get().getServiceId()).isEqualTo(serviceId);
    }

    @Test
    void existsByTeamIdAndSlug_returnsTrueWhenExists() {
        createChannel(teamId, "General", "general", ChannelType.PUBLIC);

        assertThat(channelRepository.existsByTeamIdAndSlug(teamId, "general")).isTrue();
        assertThat(channelRepository.existsByTeamIdAndSlug(teamId, "nonexistent")).isFalse();
        assertThat(channelRepository.existsByTeamIdAndSlug(otherTeamId, "general")).isFalse();
    }

    @Test
    void countByTeamId_returnsCorrectCount() {
        createChannel(teamId, "One", "one", ChannelType.PUBLIC);
        createChannel(teamId, "Two", "two", ChannelType.PRIVATE);
        createChannel(otherTeamId, "Other", "other", ChannelType.PUBLIC);

        assertThat(channelRepository.countByTeamId(teamId)).isEqualTo(2);
        assertThat(channelRepository.countByTeamId(otherTeamId)).isEqualTo(1);
    }

    @Test
    void findByTeamId_paged_returnsPagedResults() {
        for (int i = 0; i < 5; i++) {
            createChannel(teamId, "Channel " + i, "channel-" + i, ChannelType.PUBLIC);
        }

        var page = channelRepository.findByTeamId(teamId,
                org.springframework.data.domain.PageRequest.of(0, 3));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
